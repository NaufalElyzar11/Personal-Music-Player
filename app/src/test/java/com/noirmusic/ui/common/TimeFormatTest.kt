package com.noirmusic.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {

    @Test
    fun formatsMinutesHoursAndUnknownValues() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:59", formatDuration(59_999))
        assertEquals("1:01", formatDuration(61_000))
        assertEquals("1:00:00", formatDuration(3_600_000))
        assertEquals("--:--", formatDuration(-1))
        assertEquals("--:--", formatDuration(Long.MIN_VALUE + 1))
    }
}
