package com.noirmusic.playback

import androidx.media3.common.Player

enum class PlayPauseAction { PAUSE, PLAY, PREPARE_AND_PLAY, RESTART_AND_PLAY, NONE }

/** Pure playback decisions, testable on the JVM. */
object PlayerLogic {

    fun resolvePlayPause(playbackState: Int, playWhenReady: Boolean, itemCount: Int): PlayPauseAction {
        if (itemCount <= 0) return PlayPauseAction.NONE
        return when (playbackState) {
            Player.STATE_IDLE -> PlayPauseAction.PREPARE_AND_PLAY
            Player.STATE_ENDED -> PlayPauseAction.RESTART_AND_PLAY
            else -> if (playWhenReady) PlayPauseAction.PAUSE else PlayPauseAction.PLAY
        }
    }

    fun nextRepeatMode(mode: Int): Int = when (mode) {
        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
        else -> Player.REPEAT_MODE_OFF
    }

    fun needsPrepare(playbackState: Int): Boolean = playbackState == Player.STATE_IDLE
}
