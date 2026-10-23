package com.noirmusic.data.lyrics

import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsSyncTest {

    private val lines = listOf(
        LyricLine(1_000, "One"),
        LyricLine(3_000, "Two"),
        LyricLine(3_000, "Two B"),
        LyricLine(6_000, "Three"),
    )

    @Test
    fun indexAtHandlesEmptyBeforeBoundaryBetweenAfterAndDuplicates() {
        assertEquals(-1, LyricsSync.indexAt(emptyList(), 5_000))
        assertEquals(-1, LyricsSync.indexAt(lines, 0))
        assertEquals(-1, LyricsSync.indexAt(lines, 999))
        assertEquals(0, LyricsSync.indexAt(lines, 1_000))
        assertEquals(0, LyricsSync.indexAt(lines, 2_999))
        assertEquals(2, LyricsSync.indexAt(lines, 3_000)) // last among equal timestamps
        assertEquals(2, LyricsSync.indexAt(lines, 4_500))
        assertEquals(3, LyricsSync.indexAt(lines, 6_000))
        assertEquals(3, LyricsSync.indexAt(lines, 999_999))
    }
}
