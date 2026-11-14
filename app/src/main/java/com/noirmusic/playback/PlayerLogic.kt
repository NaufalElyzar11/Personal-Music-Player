package com.noirmusic.playback

import androidx.media3.common.Player
import com.noirmusic.data.model.Track
import com.noirmusic.data.preferences.PlaybackSnapshot

enum class PlayPauseAction { PAUSE, PLAY, PREPARE_AND_PLAY, RESTART_AND_PLAY, NONE }

data class RestoredQueue(
    val tracks: List<Track>,
    val startIndex: Int,
    val positionMs: Long,
)

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

    fun playNextInsertIndex(currentIndex: Int, itemCount: Int): Int {
        if (itemCount <= 0 || currentIndex < 0) return 0
        return (currentIndex + 1).coerceAtMost(itemCount)
    }

    fun canMoveItem(fromIndex: Int, toIndex: Int, itemCount: Int): Boolean =
        itemCount > 1 &&
            fromIndex in 0 until itemCount &&
            toIndex in 0 until itemCount &&
            fromIndex != toIndex

    fun <T> moveListItem(items: List<T>, fromIndex: Int, toIndex: Int): List<T> {
        if (!canMoveItem(fromIndex, toIndex, items.size)) return items
        return items.toMutableList().apply {
            val item = removeAt(fromIndex)
            add(toIndex, item)
        }
    }

    fun canClearUpcoming(currentIndex: Int, itemCount: Int): Boolean =
        itemCount > 0 && currentIndex in 0 until (itemCount - 1)

    fun resolveRestoredQueue(snapshot: PlaybackSnapshot, availableTracks: List<Track>): RestoredQueue? {
        if (snapshot.queueTrackIds.isEmpty() || availableTracks.isEmpty()) return null
        val byId = availableTracks.associateBy { it.id }
        val resolved = ArrayList<Track>(snapshot.queueTrackIds.size)
        var mappedIndex = -1
        var originalCurrentDeleted = false

        snapshot.queueTrackIds.forEachIndexed { originalIdx, trackId ->
            val track = byId[trackId]
            if (track != null) {
                resolved.add(track)
                if (originalIdx == snapshot.currentIndex) {
                    mappedIndex = resolved.lastIndex
                } else if (mappedIndex == -1 && originalIdx > snapshot.currentIndex) {
                    // Original track was deleted; resume at the next surviving track in queue.
                    mappedIndex = resolved.lastIndex
                    originalCurrentDeleted = true
                }
            } else if (originalIdx == snapshot.currentIndex) {
                originalCurrentDeleted = true
            }
        }

        if (resolved.isEmpty()) return null
        val safeIndex = if (mappedIndex in resolved.indices) mappedIndex else resolved.lastIndex
        val rawPos = if (originalCurrentDeleted) 0L else snapshot.positionMs.coerceAtLeast(0L)
        val duration = resolved[safeIndex].durationMs
        val safePos = if (duration > 0L) rawPos.coerceAtMost(duration) else rawPos
        return RestoredQueue(tracks = resolved, startIndex = safeIndex, positionMs = safePos)
    }
}
