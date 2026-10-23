package com.noirmusic.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface SleepTimerState {
    data object Inactive : SleepTimerState
    data class Active(
        val remainingSec: Long,
        val totalSec: Long,
        val isEndOfTrack: Boolean,
    ) : SleepTimerState
}

/**
 * Manages sleep timer countdown and triggers player pause upon expiration.
 * Long-lived across Activity lifecycle by running on the application scope.
 */
class SleepTimerManager(
    private val scope: CoroutineScope,
) {
    @Volatile
    private var attachedPlayer: Player? = null
    private var timerJob: Job? = null

    private val _state = MutableStateFlow<SleepTimerState>(SleepTimerState.Inactive)
    val state: StateFlow<SleepTimerState> = _state.asStateFlow()

    private val endOfTrackListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val current = _state.value
            if (current is SleepTimerState.Active && current.isEndOfTrack) {
                // When transitioning automatically to next or ending
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                    reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT
                ) {
                    onTimerFinished()
                }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val current = _state.value
            if (current is SleepTimerState.Active && current.isEndOfTrack) {
                if (playbackState == Player.STATE_ENDED) {
                    onTimerFinished()
                }
            }
        }
    }

    fun attachPlayer(player: Player) {
        attachedPlayer = player
        val current = _state.value
        if (current is SleepTimerState.Active && current.isEndOfTrack) {
            player.addListener(endOfTrackListener)
        }
    }

    fun detachPlayer() {
        attachedPlayer?.removeListener(endOfTrackListener)
        attachedPlayer = null
    }

    fun startTimer(minutes: Int) {
        if (minutes <= 0) {
            cancelTimer()
            return
        }
        val totalSec = minutes * 60L
        startCountdown(totalSec, totalSec, isEndOfTrack = false)
    }

    fun startEndOfTrackTimer() {
        timerJob?.cancel()
        timerJob = null
        attachedPlayer?.addListener(endOfTrackListener)
        _state.value = SleepTimerState.Active(
            remainingSec = 0L,
            totalSec = 0L,
            isEndOfTrack = true,
        )
    }

    fun addMinutes(minutes: Int) {
        val current = _state.value
        if (current is SleepTimerState.Active && !current.isEndOfTrack) {
            val extraSec = minutes * 60L
            val newRemaining = current.remainingSec + extraSec
            val newTotal = current.totalSec + extraSec
            startCountdown(newRemaining, newTotal, isEndOfTrack = false)
        } else {
            startTimer(minutes)
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        attachedPlayer?.removeListener(endOfTrackListener)
        _state.value = SleepTimerState.Inactive
    }

    private fun startCountdown(remainingSec: Long, totalSec: Long, isEndOfTrack: Boolean) {
        timerJob?.cancel()
        attachedPlayer?.removeListener(endOfTrackListener)
        _state.value = SleepTimerState.Active(remainingSec, totalSec, isEndOfTrack)

        timerJob = scope.launch {
            var currentRemaining = remainingSec
            while (currentRemaining > 0) {
                delay(1000L)
                currentRemaining--
                _state.value = SleepTimerState.Active(currentRemaining, totalSec, isEndOfTrack)
            }
            onTimerFinished()
        }
    }

    private fun onTimerFinished() {
        timerJob?.cancel()
        timerJob = null
        attachedPlayer?.removeListener(endOfTrackListener)
        _state.value = SleepTimerState.Inactive
        scope.launch(Dispatchers.Main) {
            try {
                attachedPlayer?.pause()
            } catch (_: Throwable) {
                // Ignore player errors during shutdown
            }
        }
    }
}
