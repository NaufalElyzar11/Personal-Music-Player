package com.noirmusic.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
private const val TAG = "PlaybackService"

/** Owns the ExoPlayer and MediaSession. The UI only talks to it through a MediaController. */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "PlaybackService onCreate")
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

        try {
            setMediaNotificationProvider(CustomMediaNotificationProvider(this, ::buildCustomLayout))
        } catch (t: Throwable) {
            Log.e(TAG, "Error configuring media notification provider", t)
        }

        player.addListener(object : Player.Listener {
            override fun onRepeatModeChanged(repeatMode: Int) {
                mediaSession?.let { it.setCustomLayout(buildCustomLayout(player)) }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                mediaSession?.let { it.setCustomLayout(buildCustomLayout(player)) }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e(TAG, "ExoPlayer onPlayerError: [${error.errorCodeName}] ${error.message}", error)
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
        Log.i(TAG, "PlaybackService onDestroy")
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
        val shuffleIconEnum = if (player.shuffleModeEnabled) CommandButton.ICON_SHUFFLE_ON else CommandButton.ICON_SHUFFLE_OFF
        val shuffleCommand = SessionCommand(ACTION_TOGGLE_SHUFFLE, Bundle())
        val shuffleButton = CommandButton.Builder(shuffleIconEnum)
            .setDisplayName(if (player.shuffleModeEnabled) "Shuffle on" else "Shuffle off")
            .setSessionCommand(shuffleCommand)
            .setCustomIconResId(shuffleIcon)
            .build()

        val (repeatIcon, repeatName, repeatIconEnum) = when (player.repeatMode) {
            Player.REPEAT_MODE_ONE -> Triple(R.drawable.ic_repeat_one, "Repeat one", CommandButton.ICON_REPEAT_ONE)
            Player.REPEAT_MODE_ALL -> Triple(R.drawable.ic_repeat_all, "Repeat all", CommandButton.ICON_REPEAT_ALL)
            else -> Triple(R.drawable.ic_repeat_off, "Repeat off", CommandButton.ICON_REPEAT_OFF)
        }
        val repeatCommand = SessionCommand(ACTION_CYCLE_REPEAT, Bundle())
        val repeatButton = CommandButton.Builder(repeatIconEnum)
            .setDisplayName(repeatName)
            .setSessionCommand(repeatCommand)
            .setCustomIconResId(repeatIcon)
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
            return try {
                val layout = customLayoutProvider(session.player)
                val shuffleButton = layout.firstOrNull { it.sessionCommand?.customAction == ACTION_TOGGLE_SHUFFLE }
                val repeatButton = layout.firstOrNull { it.sessionCommand?.customAction == ACTION_CYCLE_REPEAT }

                val defaultButtons = super.getMediaButtons(session, playerCommands, customLayout, showPauseButton)
                val result = mutableListOf<CommandButton>()
                if (shuffleButton != null) result.add(shuffleButton)
                result.addAll(defaultButtons)
                if (repeatButton != null) result.add(repeatButton)
                ImmutableList.copyOf(result)
            } catch (t: Throwable) {
                Log.e(TAG, "Error creating notification media buttons, falling back to default", t)
                super.getMediaButtons(session, playerCommands, customLayout, showPauseButton)
            }
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
            Log.d(TAG, "SessionCallback onConnect from package: ${controller.packageName}")
            val availableSessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand(ACTION_TOGGLE_SHUFFLE, Bundle()))
                .add(SessionCommand(ACTION_CYCLE_REPEAT, Bundle()))
                .build()

            val availablePlayerCommands = Player.Commands.Builder()
                .addAll(session.player.availableCommands)
                .add(Player.COMMAND_PLAY_PAUSE)
                .add(Player.COMMAND_PREPARE)
                .add(Player.COMMAND_STOP)
                .add(Player.COMMAND_SEEK_TO_DEFAULT_POSITION)
                .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_MEDIA_ITEM)
                .add(Player.COMMAND_SET_SPEED_AND_PITCH)
                .add(Player.COMMAND_SET_SHUFFLE_MODE)
                .add(Player.COMMAND_SET_REPEAT_MODE)
                .add(Player.COMMAND_CHANGE_MEDIA_ITEMS)
                .add(Player.COMMAND_GET_CURRENT_MEDIA_ITEM)
                .add(Player.COMMAND_GET_TIMELINE)
                .add(Player.COMMAND_GET_METADATA)
                .add(Player.COMMAND_SET_MEDIA_ITEM)
                .build()

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller)
                .setAvailablePlayerCommands(availablePlayerCommands)
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
            Log.d(TAG, "SessionCallback onCustomCommand: ${customCommand.customAction}")
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
            Log.d(TAG, "onSetMediaItems: ${mediaItems.size} items, startIndex=$startIndex, startPos=$startPositionMs")
            val resolved = mediaItems.mapNotNull { resolveForPlayback(it, collectionUri) }
            Log.d(TAG, "onSetMediaItems: resolved ${resolved.size} items")
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
        ): ListenableFuture<MutableList<MediaItem>> {
            Log.d(TAG, "onAddMediaItems: ${mediaItems.size} items")
            val resolved = mediaItems.mapNotNull { resolveForPlayback(it, collectionUri) }
            return Futures.immediateFuture(resolved.toMutableList())
        }
    }
}
