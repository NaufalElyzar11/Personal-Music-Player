package com.noirmusic.data.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsMatcherTest {

    private fun query(
        title: String = "The Chain",
        artist: String? = "Fleetwood Mac",
        album: String? = "Rumours",
        durationSec: Int? = 270,
    ) = LyricsQuery(
        mediaId = "1",
        title = title,
        artist = artist,
        album = album,
        durationSec = durationSec,
        titleFromFileName = false,
    )

    private fun dto(
        id: Long = 1,
        trackName: String? = "The Chain",
        artistName: String? = "Fleetwood Mac",
        albumName: String? = "Rumours",
        duration: Double? = 270.0,
        instrumental: Boolean = false,
        plain: String? = "Listen to the wind blow",
        synced: String? = "[00:10.00]Listen to the wind blow",
    ) = LrclibRecordDto(
        id = id,
        trackName = trackName,
        artistName = artistName,
        albumName = albumName,
        duration = duration,
        instrumental = instrumental,
        plainLyrics = plain,
        syncedLyrics = synced,
    )

    @Test
    fun acceptsExactMatchAndRejectsWrongArtistOrDistantDuration() {
        val q = query()
        assertTrue(LyricsMatcher.isAcceptable(q, dto()))
        assertFalse(LyricsMatcher.isAcceptable(q, dto(artistName = "Someone Else")))
        assertTrue(LyricsMatcher.isAcceptable(q, dto(duration = 273.0))) // 3 s diff
        assertFalse(LyricsMatcher.isAcceptable(q, dto(duration = 274.0))) // 4 s diff
    }

    @Test
    fun handlesUnknownDurationUnknownArtistMultiArtistAndEmptyPayload() {
        assertTrue(LyricsMatcher.isAcceptable(query(durationSec = null), dto(duration = 500.0)))

        val unknownArtist = query(artist = null, durationSec = 200)
        assertTrue(LyricsMatcher.isAcceptable(unknownArtist, dto(duration = 202.0)))
        assertFalse(LyricsMatcher.isAcceptable(unknownArtist, dto(duration = 203.0)))
        assertFalse(LyricsMatcher.isAcceptable(unknownArtist, dto(trackName = "The Chain Extended", duration = 200.0)))

        val collab = query(artist = "Artist A & Artist B")
        assertTrue(LyricsMatcher.isAcceptable(collab, dto(artistName = "Artist A")))

        assertFalse(LyricsMatcher.isAcceptable(query(), dto(plain = null, synced = null, instrumental = false)))
    }

    @Test
    fun selectBestSkipsInvalidFirstResultAndPrefersSyncedThenCloserDuration() {
        val q = query(durationSec = 270)
        val wrongFirst = dto(id = 1, artistName = "Wrong Artist")
        val plainClose = dto(id = 2, duration = 270.0, synced = null, plain = "Plain text")
        val syncedFarther = dto(id = 3, duration = 272.0, synced = "[00:01.00]Synced", plain = "Plain text")
        val syncedClosest = dto(id = 4, duration = 271.0, synced = "[00:01.00]Synced best", plain = "Plain text")

        val chosen = LyricsMatcher.selectBest(q, listOf(wrongFirst, plainClose, syncedFarther, syncedClosest))
        assertEquals(4L, chosen?.id)
    }

    @Test
    fun selectBestReturnsInstrumentalWhenOnlyInstrumentalIsAcceptableAndNullWhenNone() {
        val q = query()
        val inst = dto(id = 9, instrumental = true, plain = null, synced = null)
        assertEquals(9L, LyricsMatcher.selectBest(q, listOf(inst))?.id)
        assertNull(LyricsMatcher.selectBest(q, listOf(dto(artistName = "Other"))))
    }

    @Test
    fun acceptsRecordUsingNameWhenTrackNameIsNull() {
        val q = query(title = "Dreams")
        val recordWithName = LrclibRecordDto(
            id = 10,
            trackName = null,
            name = "Dreams",
            artistName = "Fleetwood Mac",
            duration = 270.0,
            syncedLyrics = "[00:05.00]Now here you go again",
        )
        assertTrue(LyricsMatcher.isAcceptable(q, recordWithName))
        val chosen = LyricsMatcher.selectBest(q, listOf(recordWithName))
        assertEquals(10L, chosen?.id)
    }
}
