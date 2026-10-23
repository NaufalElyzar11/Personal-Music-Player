package com.noirmusic.ui.player

import androidx.media3.common.Player
import com.noirmusic.data.lyrics.LyricLine
import com.noirmusic.data.lyrics.LyricsError
import com.noirmusic.data.lyrics.LyricsQuery
import com.noirmusic.data.lyrics.LyricsResult
import com.noirmusic.data.model.Track

data class NowPlaying(
    val mediaId: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val durationMs: Long,
    val titleFromFileName: Boolean,
) {
    val displayArtist: String get() = artist ?: Track.UNKNOWN_ARTIST

    fun toLyricsQuery(): LyricsQuery = LyricsQuery(
        mediaId = mediaId,
        title = title,
        artist = artist,
        album = album,
        durationSec = LyricsQuery.durationSecFromMs(durationMs),
        titleFromFileName = titleFromFileName,
    )
}

data class PlaybackUiState(
    val isConnected: Boolean = false,
    val nowPlaying: NowPlaying? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val playbackState: Int = Player.STATE_IDLE,
    val playWhenReady: Boolean = false,
    val durationMs: Long = 0L,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val queueIndex: Int = -1,
    val queueSize: Int = 0,
    val queue: List<NowPlaying> = emptyList(),
    val errorMessage: String? = null,
)

sealed interface LyricsUiState {
    data object NoTrack : LyricsUiState
    data object Loading : LyricsUiState
    data class Synced(val lines: List<LyricLine>) : LyricsUiState
    data class Plain(val lines: List<String>) : LyricsUiState
    data object Instrumental : LyricsUiState
    data object NotFound : LyricsUiState
    data object NotSearchable : LyricsUiState
    data class Error(val error: LyricsError) : LyricsUiState
}

enum class NowPlayingPane { COVER, LYRICS }

fun LyricsResult.toUiState(): LyricsUiState = when (this) {
    is LyricsResult.Synced -> LyricsUiState.Synced(lines)
    is LyricsResult.Plain -> LyricsUiState.Plain(lines)
    LyricsResult.Instrumental -> LyricsUiState.Instrumental
    LyricsResult.NotFound -> LyricsUiState.NotFound
    LyricsResult.NotSearchable -> LyricsUiState.NotSearchable
    is LyricsResult.Failed -> LyricsUiState.Error(error)
}
