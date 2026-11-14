package com.noirmusic.playback

import androidx.media3.common.Player
import com.noirmusic.data.model.Track
import com.noirmusic.data.preferences.PlaybackSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test
    fun calculatesPlayNextInsertIndex() {
        assertEquals(0, PlayerLogic.playNextInsertIndex(currentIndex = -1, itemCount = 0))
        assertEquals(0, PlayerLogic.playNextInsertIndex(currentIndex = 0, itemCount = 0))
        assertEquals(0, PlayerLogic.playNextInsertIndex(currentIndex = -1, itemCount = 3))
        assertEquals(1, PlayerLogic.playNextInsertIndex(currentIndex = 0, itemCount = 3))
        assertEquals(3, PlayerLogic.playNextInsertIndex(currentIndex = 2, itemCount = 3))
        assertEquals(3, PlayerLogic.playNextInsertIndex(currentIndex = 5, itemCount = 3))
    }

    @Test
    fun movesListItemAndValidatesBounds() {
        val items = listOf("a", "b", "c", "d")
        assertEquals(listOf("b", "c", "a", "d"), PlayerLogic.moveListItem(items, 0, 2))
        assertEquals(listOf("d", "a", "b", "c"), PlayerLogic.moveListItem(items, 3, 0))
        assertEquals(items, PlayerLogic.moveListItem(items, 1, 1))
        assertEquals(items, PlayerLogic.moveListItem(items, -1, 2))
        assertEquals(items, PlayerLogic.moveListItem(items, 1, 4))
        assertEquals(listOf("only"), PlayerLogic.moveListItem(listOf("only"), 0, 0))
    }

    @Test
    fun checksCanClearUpcoming() {
        assertFalse(PlayerLogic.canClearUpcoming(currentIndex = -1, itemCount = 0))
        assertFalse(PlayerLogic.canClearUpcoming(currentIndex = 0, itemCount = 1))
        assertFalse(PlayerLogic.canClearUpcoming(currentIndex = 2, itemCount = 3))
        assertTrue(PlayerLogic.canClearUpcoming(currentIndex = 0, itemCount = 3))
        assertTrue(PlayerLogic.canClearUpcoming(currentIndex = 1, itemCount = 3))
    }

    private fun track(id: Long, durationMs: Long = 180_000L) = Track(
        id = id,
        title = "Track $id",
        artist = "Artist",
        album = "Album",
        durationMs = durationMs,
        dateAddedSec = 100L,
        titleFromFileName = false,
    )

    @Test
    fun resolvesRestoredQueueAndAdjustsForDeletedTracks() {
        val t1 = track(1L)
        val t2 = track(2L, durationMs = 120_000L)
        val t3 = track(3L)

        // Normal restore clamps position to track duration.
        val snap1 = PlaybackSnapshot(
            queueTrackIds = listOf(1L, 2L, 3L),
            currentIndex = 1,
            positionMs = 150_000L,
            shuffleEnabled = true,
            repeatMode = Player.REPEAT_MODE_ALL,
        )
        val r1 = PlayerLogic.resolveRestoredQueue(snap1, listOf(t1, t2, t3))!!
        assertEquals(listOf(t1, t2, t3), r1.tracks)
        assertEquals(1, r1.startIndex)
        assertEquals(120_000L, r1.positionMs)

        // Track before current was deleted: index shifts left, position kept.
        val snap2 = PlaybackSnapshot(listOf(1L, 2L, 3L), currentIndex = 2, positionMs = 45_000L, false, 0)
        val r2 = PlayerLogic.resolveRestoredQueue(snap2, listOf(t2, t3))!!
        assertEquals(listOf(t2, t3), r2.tracks)
        assertEquals(1, r2.startIndex)
        assertEquals(45_000L, r2.positionMs)

        // Current track deleted: advances to next surviving track at position 0.
        val snap3 = PlaybackSnapshot(listOf(1L, 2L, 3L), currentIndex = 1, positionMs = 45_000L, false, 0)
        val r3 = PlayerLogic.resolveRestoredQueue(snap3, listOf(t1, t3))!!
        assertEquals(listOf(t1, t3), r3.tracks)
        assertEquals(1, r3.startIndex)
        assertEquals(0L, r3.positionMs)

        // No surviving tracks returns null.
        assertNull(PlayerLogic.resolveRestoredQueue(snap1, emptyList()))
        assertNull(PlayerLogic.resolveRestoredQueue(snap1, listOf(track(99L))))
    }
}
