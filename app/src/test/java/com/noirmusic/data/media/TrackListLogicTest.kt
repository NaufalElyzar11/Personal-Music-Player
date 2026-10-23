package com.noirmusic.data.media

import com.noirmusic.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.Collator
import java.util.Locale

class TrackListLogicTest {

    private val collator: Collator = Collator.getInstance(Locale.US).apply { strength = Collator.PRIMARY }

    private val tracks = listOf(
        Track(1, "Zebra", "Beyoncé", "Renaissance", 200_000, dateAddedSec = 10, titleFromFileName = false),
        Track(2, "Alpha", null, "Demo", 180_000, dateAddedSec = 30, titleFromFileName = false),
        Track(3, "Beta", "Air", "Moon Safari", 210_000, dateAddedSec = 20, titleFromFileName = false),
    )

    @Test
    fun filtersCaseAndDiacriticInsensitivelyAndSortsByAllThreeOrders() {
        val bySearch = TrackListLogic.apply(tracks, "beyonce", SortOrder.TITLE, collator)
        assertEquals(listOf(1L), bySearch.map { it.id })

        val byAlbum = TrackListLogic.apply(tracks, "safari", SortOrder.TITLE, collator)
        assertEquals(listOf(3L), byAlbum.map { it.id })

        val byTitle = TrackListLogic.apply(tracks, "", SortOrder.TITLE, collator)
        assertEquals(listOf(2L, 3L, 1L), byTitle.map { it.id })

        // Unknown artist sorted last
        val byArtist = TrackListLogic.apply(tracks, "", SortOrder.ARTIST, collator)
        assertEquals(listOf(3L, 1L, 2L), byArtist.map { it.id })

        val byRecent = TrackListLogic.apply(tracks, "", SortOrder.RECENTLY_ADDED, collator)
        assertEquals(listOf(2L, 3L, 1L), byRecent.map { it.id })
    }
}
