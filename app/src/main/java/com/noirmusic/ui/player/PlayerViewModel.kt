package com.noirmusic.ui.player

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.noirmusic.data.lyrics.LyricsRepository
import com.noirmusic.data.lyrics.LyricsSync
import com.noirmusic.data.model.Track
import com.noirmusic.playback.EXTRA_TITLE_FROM_FILE_NAME
import com.noirmusic.playback.PlayPauseAction
import com.noirmusic.playback.PlaybackConnection
import com.noirmusic.playback.PlayerLogic
import com.noirmusic.playback.SleepTimerManager
import com.noirmusic.playback.SleepTimerState
import com.noirmusic.playback.toMediaItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

private class ArtworkKey(val mediaId: String?, val data: ByteArray?) {
    override fun equals(other: Any?): Boolean =
        other is ArtworkKey && mediaId == other.mediaId &&
            ((data == null && other.data == null) || (data != null && other.data != null && data.contentEquals(other.data)))

    override fun hashCode(): Int = 31 * (mediaId?.hashCode() ?: 0) + (data?.contentHashCode() ?: 0)
}

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModel(
    private val connection: PlaybackConnection,
    private val lyricsRepository: LyricsRepository,
    private val sleepTimerManager: SleepTimerManager,
) : ViewModel() {

    private var controller: MediaController? = null
    private val controllerReady = CompletableDeferred<MediaController?>()

    private val _playbackState = MutableStateFlow(PlaybackUiState())
    val playbackState: StateFlow<PlaybackUiState> = _playbackState.asStateFlow()

    val sleepTimerState: StateFlow<SleepTimerState> = sleepTimerManager.state

    private val _nowPlayingPane = MutableStateFlow(NowPlayingPane.COVER)
    val nowPlayingPane: StateFlow<NowPlayingPane> = _nowPlayingPane.asStateFlow()

    private val positionInvalidations = MutableSharedFlow<Unit>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    ).apply { tryEmit(Unit) }

    private val retryNonce = MutableStateFlow(0L)
    private val artworkSource = MutableStateFlow(ArtworkKey(null, null))

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publish(player)
            if (events.containsAny(
                    Player.EVENT_POSITION_DISCONTINUITY,
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_PLAYBACK_STATE_CHANGED,
                    Player.EVENT_IS_PLAYING_CHANGED,
                )
            ) {
                positionInvalidations.tryEmit(Unit)
            }
            if (events.containsAny(Player.EVENT_MEDIA_METADATA_CHANGED, Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                artworkSource.value = ArtworkKey(player.currentMediaItem?.mediaId, player.mediaMetadata.artworkData)
            }
            if (events.contains(Player.EVENT_PLAYER_ERROR)) {
                Log.e("PlayerViewModel", "Player error: ${player.playerError?.errorCodeName}: ${player.playerError?.message}", player.playerError)
            }
        }
    }

    init {
        viewModelScope.launch {
            ensureController()
        }
    }

    private suspend fun ensureController(): MediaController? {
        controller?.takeIf { it.isConnected }?.let { return it }
        return try {
            Log.d("PlayerViewModel", "Connecting to PlaybackConnection...")
            val c = connection.await()
            controller = c
            c.addListener(listener)
            publish(c)
            artworkSource.value = ArtworkKey(c.currentMediaItem?.mediaId, c.mediaMetadata.artworkData)
            positionInvalidations.tryEmit(Unit)
            if (!controllerReady.isCompleted) {
                controllerReady.complete(c)
            }
            Log.d("PlayerViewModel", "Successfully connected to PlaybackService via MediaController")
            c
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Log.e("PlayerViewModel", "Failed to connect to PlaybackService", t)
            _playbackState.value = PlaybackUiState(
                isConnected = false,
                errorMessage = "Gagal terhubung ke pemutar audio: ${t.localizedMessage ?: t.javaClass.simpleName}",
            )
            if (!controllerReady.isCompleted) {
                controllerReady.complete(null)
            }
            null
        }
    }

    val positionMs: StateFlow<Long> = merge(
        _playbackState.map { it.isPlaying }.distinctUntilChanged().flatMapLatest { playing ->
            if (playing) {
                flow {
                    while (true) {
                        emit(Unit)
                        delay(200L)
                    }
                }
            } else {
                flowOf(Unit)
            }
        },
        positionInvalidations,
    ).map {
        controller?.currentPosition?.coerceAtLeast(0L) ?: 0L
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val lyricsState: StateFlow<LyricsUiState> = combine(
        _playbackState.map { it.nowPlaying?.toLyricsQuery() }.distinctUntilChanged(),
        retryNonce,
    ) { query, nonce -> query to nonce }
        .flatMapLatest { (query, _) ->
            if (query == null) {
                flowOf(LyricsUiState.NoTrack)
            } else {
                val cached = lyricsRepository.cached(query)
                if (cached != null) {
                    flowOf(cached.toUiState())
                } else {
                    flow {
                        emit(LyricsUiState.Loading)
                        delay(500L)
                        emit(lyricsRepository.getLyrics(query).toUiState())
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LyricsUiState.NoTrack)

    val activeLineIndex: StateFlow<Int> = combine(lyricsState, positionMs) { state, pos ->
        if (state is LyricsUiState.Synced) LyricsSync.indexAt(state.lines, pos) else -1
    }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), -1)

    val artwork: StateFlow<ImageBitmap?> = artworkSource.mapLatest { key ->
        val bytes = key.data ?: return@mapLatest null
        withContext(Dispatchers.Default) {
            try {
                decodeDownsampled(bytes, maxDim = 1024)?.asImageBitmap()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                null
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setNowPlayingPane(pane: NowPlayingPane) {
        _nowPlayingPane.value = pane
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerManager.startTimer(minutes)
    }

    fun startSleepTimerEndOfTrack() {
        sleepTimerManager.startEndOfTrackTimer()
    }

    fun addSleepTimerMinutes(minutes: Int) {
        sleepTimerManager.addMinutes(minutes)
    }

    fun cancelSleepTimer() {
        sleepTimerManager.cancelTimer()
    }

    fun retryLyrics() {
        retryNonce.value += 1L
    }

    fun playQueue(tracks: List<Track>, startIndex: Int) {
        if (tracks.isEmpty() || startIndex !in tracks.indices) {
            Log.w("PlayerViewModel", "playQueue rejected: tracks.size=${tracks.size}, startIndex=$startIndex")
            return
        }
        Log.d("PlayerViewModel", "playQueue: ${tracks.size} tracks, starting at $startIndex (${tracks[startIndex].title})")
        withController { c ->
            c.setMediaItems(tracks.map { it.toMediaItem() }, startIndex, 0L)
            c.prepare()
            c.play()
        }
    }

    fun togglePlayPause() = withController { c ->
        when (PlayerLogic.resolvePlayPause(c.playbackState, c.playWhenReady, c.mediaItemCount)) {
            PlayPauseAction.PAUSE -> c.pause()
            PlayPauseAction.PLAY -> c.play()
            PlayPauseAction.PREPARE_AND_PLAY -> {
                c.prepare()
                c.play()
            }
            PlayPauseAction.RESTART_AND_PLAY -> {
                c.seekToDefaultPosition(0)
                c.play()
            }
            PlayPauseAction.NONE -> Unit
        }
    }

    fun next() = withController { c ->
        if (PlayerLogic.needsPrepare(c.playbackState)) c.prepare()
        c.seekToNext()
    }

    fun previous() = withController { c ->
        if (PlayerLogic.needsPrepare(c.playbackState)) c.prepare()
        c.seekToPrevious()
    }

    fun seekTo(positionMs: Long) = withController { c ->
        c.seekTo(positionMs.coerceAtLeast(0L))
        positionInvalidations.tryEmit(Unit)
    }

    fun skipToQueueItem(index: Int) = withController { c ->
        if (index in 0 until c.mediaItemCount) {
            c.seekToDefaultPosition(index)
            c.play()
        }
    }

    fun toggleShuffle() = withController { c ->
        c.shuffleModeEnabled = !c.shuffleModeEnabled
    }

    fun cycleRepeat() = withController { c ->
        c.repeatMode = PlayerLogic.nextRepeatMode(c.repeatMode)
    }

    private inline fun withController(crossinline block: (MediaController) -> Unit) {
        viewModelScope.launch {
            val c = controller ?: ensureController() ?: run {
                Log.e("PlayerViewModel", "withController failed: MediaController is not available")
                return@launch
            }
            try {
                block(c)
            } catch (t: Throwable) {
                Log.e("PlayerViewModel", "Exception inside withController block", t)
            }
        }
    }

    private fun publish(p: Player) {
        val current = p.currentMediaItem?.toNowPlaying()
        val playerDuration = p.duration.takeIf { it > 0 }
        val durationMs = playerDuration ?: current?.durationMs ?: 0L
        val queueItems = try {
            (0 until p.mediaItemCount).mapNotNull { idx ->
                p.getMediaItemAt(idx).toNowPlaying()
            }
        } catch (_: Throwable) {
            emptyList()
        }
        Log.d("PlayerViewModel", "publish: current='${current?.title}', isPlaying=${p.isPlaying}, playbackState=${p.playbackState}, queueSize=${p.mediaItemCount}")
        _playbackState.value = PlaybackUiState(
            isConnected = true,
            nowPlaying = current,
            isPlaying = p.isPlaying,
            isBuffering = p.playbackState == Player.STATE_BUFFERING,
            playbackState = p.playbackState,
            playWhenReady = p.playWhenReady,
            durationMs = durationMs,
            hasPrevious = p.mediaItemCount > 0,
            hasNext = p.hasNextMediaItem(),
            shuffleEnabled = p.shuffleModeEnabled,
            repeatMode = p.repeatMode,
            queueIndex = if (p.mediaItemCount > 0) p.currentMediaItemIndex else -1,
            queueSize = p.mediaItemCount,
            queue = queueItems,
            errorMessage = p.playerError?.let { it.localizedMessage ?: it.errorCodeName },
        )
    }

    private fun MediaItem.toNowPlaying(): NowPlaying? {
        if (mediaId.isBlank()) return null
        val m = mediaMetadata
        val title = m.title?.toString()?.takeIf { it.isNotBlank() }
            ?: m.displayTitle?.toString()?.takeIf { it.isNotBlank() }
            ?: "Track $mediaId"
        return NowPlaying(
            mediaId = mediaId,
            title = title,
            artist = m.artist?.toString()?.takeIf { it.isNotBlank() },
            album = m.albumTitle?.toString()?.takeIf { it.isNotBlank() },
            durationMs = (m.durationMs ?: 0L).coerceAtLeast(0L),
            titleFromFileName = m.extras?.getBoolean(EXTRA_TITLE_FROM_FILE_NAME, false) ?: false,
        )
    }

    override fun onCleared() {
        controller?.removeListener(listener)
        controller = null
        connection.release()
        super.onCleared()
    }

    companion object {
        private fun decodeDownsampled(bytes: ByteArray, maxDim: Int): android.graphics.Bitmap? {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            val w = bounds.outWidth
            val h = bounds.outHeight
            if (w <= 0 || h <= 0) return null
            var sample = 1
            while (max(w / sample, h / sample) > maxDim) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        }
    }
}
