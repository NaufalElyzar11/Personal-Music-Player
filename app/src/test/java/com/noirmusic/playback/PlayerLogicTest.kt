package com.noirmusic.playback

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerLogicTest {

    @Test
    fun resolvesPlayPauseAcrossAllPlayerStates() {
        assertEquals(
            PlayPauseAction.PAUSE,
            PlayerLogic.resolvePlayPause(Player.STATE_READY, playWhenReady = true, itemCount = 3),
        )
        assertEquals(
            PlayPauseAction.PLAY,
            PlayerLogic.resolvePlayPause(Player.STATE_READY, playWhenReady = false, itemCount = 3),
        )
        assertEquals(
            PlayPauseAction.PAUSE,
            PlayerLogic.resolvePlayPause(Player.STATE_BUFFERING, playWhenReady = true, itemCount = 3),
        )
        assertEquals(
            PlayPauseAction.PREPARE_AND_PLAY,
            PlayerLogic.resolvePlayPause(Player.STATE_IDLE, playWhenReady = false, itemCount = 3),
        )
        assertEquals(
            PlayPauseAction.NONE,
            PlayerLogic.resolvePlayPause(Player.STATE_IDLE, playWhenReady = false, itemCount = 0),
        )
        assertEquals(
            PlayPauseAction.RESTART_AND_PLAY,
            PlayerLogic.resolvePlayPause(Player.STATE_ENDED, playWhenReady = true, itemCount = 3),
        )
    }

    @Test
    fun cyclesRepeatModeAndChecksNeedsPrepare() {
        assertEquals(Player.REPEAT_MODE_ALL, PlayerLogic.nextRepeatMode(Player.REPEAT_MODE_OFF))
        assertEquals(Player.REPEAT_MODE_ONE, PlayerLogic.nextRepeatMode(Player.REPEAT_MODE_ALL))
        assertEquals(Player.REPEAT_MODE_OFF, PlayerLogic.nextRepeatMode(Player.REPEAT_MODE_ONE))
        assertTrue(PlayerLogic.needsPrepare(Player.STATE_IDLE))
        assertFalse(PlayerLogic.needsPrepare(Player.STATE_READY))
    }
}
