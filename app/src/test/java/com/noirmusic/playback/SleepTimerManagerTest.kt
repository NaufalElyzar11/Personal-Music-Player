package com.noirmusic.playback

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SleepTimerManagerTest {

    @Test
    fun startTimerAndCountdownUpdates() = runTest {
        val manager = SleepTimerManager(this)

        manager.startTimer(1) // 1 minute = 60s
        val initial = manager.state.value
        assertTrue(initial is SleepTimerState.Active)
        assertEquals(60L, (initial as SleepTimerState.Active).remainingSec)

        advanceTimeBy(5100L) // advance 5 seconds
        val updated = manager.state.value as SleepTimerState.Active
        assertEquals(55L, updated.remainingSec)

        manager.addMinutes(1) // add 60s
        val afterAdd = manager.state.value as SleepTimerState.Active
        assertEquals(115L, afterAdd.remainingSec)

        manager.cancelTimer()
        assertEquals(SleepTimerState.Inactive, manager.state.value)
    }

    @Test
    fun endOfTrackTimerSetsCorrectState() {
        val testScope = TestScope()
        val manager = SleepTimerManager(testScope)

        manager.startEndOfTrackTimer()
        val state = manager.state.value
        assertTrue(state is SleepTimerState.Active)
        assertTrue((state as SleepTimerState.Active).isEndOfTrack)

        manager.cancelTimer()
        assertEquals(SleepTimerState.Inactive, manager.state.value)
    }
}
