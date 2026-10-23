package com.noirmusic.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.noirmusic.MainActivity
import com.noirmusic.NoirApplication
import com.noirmusic.R
import com.noirmusic.data.media.MediaStoreTrackRepository

const val ACTION_TOGGLE_SHUFFLE = "com.noirmusic.ACTION_TOGGLE_SHUFFLE"
const val ACTION_CYCLE_REPEAT = "com.noirmusic.ACTION_CYCLE_REPEAT"

/** Owns the ExoPlayer and MediaSession. The UI only talks to it through a MediaController. */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(MainActivity.EXTRA_OPEN_PLAYER, true),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .setCallback(
                SessionCallback(
                    collectionUri = MediaStoreTrackRepository.audioCollectionUri(),
                    customLayoutProvider = ::buildCustomLayout,
                ),
            )
            .build()

        setMediaNotificationProvider(CustomMediaNotificationProvider(this, ::buildCustomLayout))

        player.addListener(object : Player.Listener {
            override fun onRepeatModeChanged(repeatMode: Int) {
                mediaSession?.setCustomLayout(buildCustomLayout(player))
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                mediaSession?.setCustomLayout(buildCustomLayout(player))
            }
        })

        val container = (application as? NoirApplication)?.container
        container?.sleepTimerManager?.attachPlayer(player)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null ||
            !player.playWhenReady ||
            player.mediaItemCount == 0 ||
            player.playbackState == Player.STATE_ENDED
        ) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        val container = (application as? NoirApplication)?.container
        container?.sleepTimerManager?.detachPlayer()

        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    private fun buildCustomLayout(player: Player): ImmutableList<CommandButton> {
        val shuffleIcon = if (player.shuffleModeEnabled) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle_off
        val shuffleCommand = SessionCommand(ACTION_TOGGLE_SHUFFLE, Bundle.EMPTY)
        val shuffleButton = CommandButton.Builder(shuffleIcon)
            .setDisplayName(if (player.shuffleModeEnabled) "Shuffle on" else "Shuffle off")
            .setSessionCommand(shuffleCommand)
            .build()

        val (repeatIcon, repeatName) = when (player.repeatMode) {
            Player.REPEAT_MODE_ONE -> R.drawable.ic_repeat_one to "Repeat one"
            Player.REPEAT_MODE_ALL -> R.drawable.ic_repeat_all to "Repeat all"
            else -> R.drawable.ic_repeat_off to "Repeat off"
        }
        val repeatCommand = SessionCommand(ACTION_CYCLE_REPEAT, Bundle.EMPTY)
        val repeatButton = CommandButton.Builder(repeatIcon)
            .setDisplayName(repeatName)
            .setSessionCommand(repeatCommand)
            .build()

        return ImmutableList.of(shuffleButton, repeatButton)
    }

    private class CustomMediaNotificationProvider(
        context: Context,
        private val customLayoutProvider: (Player) -> ImmutableList<CommandButton>,
    ) : DefaultMediaNotificationProvider(context) {

        override fun getMediaButtons(
            session: MediaSession,
            playerCommands: Player.Commands,
            customLayout: ImmutableList<CommandButton>,
            showPauseButton: Boolean,
        ): ImmutableList<CommandButton> {
            val layout = customLayoutProvider(session.player)
            val shuffleButton = layout.firstOrNull { it.sessionCommand?.customAction == ACTION_TOGGLE_SHUFFLE }
            val repeatButton = layout.firstOrNull { it.sessionCommand?.customAction == ACTION_CYCLE_REPEAT }

            val defaultButtons = super.getMediaButtons(session, playerCommands, customLayout, showPauseButton)
            val result = mutableListOf<CommandButton>()
            if (shuffleButton != null) result.add(shuffleButton)
            result.addAll(defaultButtons)
            if (repeatButton != null) result.add(repeatButton)
            return ImmutableList.copyOf(result)
        }
    }

    private class SessionCallback(
        private val collectionUri: Uri,
        private val customLayoutProvider: (Player) -> ImmutableList<CommandButton>,
    ) : MediaSession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val connectionResult = super.onConnect(session, controller)
            val availableSessionCommands = connectionResult.availableSessionCommands.buildUpon()
                .add(SessionCommand(ACTION_TOGGLE_SHUFFLE, Bundle.EMPTY))
                .add(SessionCommand(ACTION_CYCLE_REPEAT, Bundle.EMPTY))
                .build()

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailablePlayerCommands(connectionResult.availablePlayerCommands)
                .setAvailableSessionCommands(availableSessionCommands)
                .setCustomLayout(customLayoutProvider(session.player))
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            val player = session.player
            when (customCommand.customAction) {
                ACTION_TOGGLE_SHUFFLE -> {
                    player.shuffleModeEnabled = !player.shuffleModeEnabled
                    session.setCustomLayout(customLayoutProvider(player))
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                ACTION_CYCLE_REPEAT -> {
                    player.repeatMode = PlayerLogic.nextRepeatMode(player.repeatMode)
                    session.setCustomLayout(customLayoutProvider(player))
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val resolved = mediaItems.mapNotNull { resolveForPlayback(it, collectionUri) }
            val validIndex = if (startIndex in resolved.indices) startIndex else 0
            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(
                    resolved,
                    validIndex,
                    startPositionMs.coerceAtLeast(0L),
                ),
            )
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> =
            Futures.immediateFuture(mediaItems.mapNotNull { resolveForPlayback(it, collectionUri) }.toMutableList())
    }
}
