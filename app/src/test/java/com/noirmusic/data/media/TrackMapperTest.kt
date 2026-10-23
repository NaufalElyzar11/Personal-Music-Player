package com.noirmusic.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackMapperTest {

    @Test
    fun mapsMissingTagsUnknownMarkersAndNegativeDuration() {
        val fromFile = TrackMapper.fromRow(
            id = 5L,
            title = "   ",
            artist = "<unknown>",
            album = "<unknown>",
            durationMs = -100L,
            dateAddedSec = null,
            displayName = "01 - Track Name.flac",
        )
        assertEquals("01 - Track Name", fromFile.title)
        assertTrue(fromFile.titleFromFileName)
        assertNull(fromFile.artist)
        assertNull(fromFile.album)
        assertEquals(0L, fromFile.durationMs)
        assertEquals(0L, fromFile.dateAddedSec)

        val allMissing = TrackMapper.fromRow(1L, null, null, null, null, null, "   ")
        assertEquals(TrackMapper.UNKNOWN_TITLE, allMissing.title)
        assertTrue(allMissing.titleFromFileName)

        val tagged = TrackMapper.fromRow(2L, " Song ", " Artist ", " Album ", 180_000L, 99L, "file.mp3")
        assertEquals("Song", tagged.title)
        assertFalse(tagged.titleFromFileName)
        assertEquals("Artist", tagged.artist)
        assertEquals("Album", tagged.album)
    }
}
