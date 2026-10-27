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

    @Test
    fun resolvesFastScrollSectionCharAndTargetIndicesAcrossDigitsAndLetters() {
        val sample = listOf(
            Track(10, "1979", "Smashing Pumpkins", "Mellon Collie", 260_000, dateAddedSec = 1, titleFromFileName = false),
            Track(11, "505", "Arctic Monkeys", "Favourite Worst Nightmare", 250_000, dateAddedSec = 2, titleFromFileName = false),
            Track(12, "Alpha", "Air", "Moon Safari", 180_000, dateAddedSec = 3, titleFromFileName = false),
            Track(13, "Midnight City", "M83", "Hurry Up", 240_000, dateAddedSec = 4, titleFromFileName = false),
            Track(14, "Zebra", "Beach House", "Teen Dream", 290_000, dateAddedSec = 5, titleFromFileName = false),
        )
        val sortedByTitle = TrackListLogic.sortTracks(sample, SortOrder.TITLE, collator)

        assertEquals(36, TrackListLogic.FAST_SCROLL_SYMBOLS.size)
        assertEquals('0', TrackListLogic.FAST_SCROLL_SYMBOLS.first())
        assertEquals('Z', TrackListLogic.FAST_SCROLL_SYMBOLS.last())

        assertEquals('1', TrackListLogic.sectionCharFor(sortedByTitle[0], SortOrder.TITLE))
        assertEquals('5', TrackListLogic.sectionCharFor(sortedByTitle[1], SortOrder.TITLE))
        assertEquals('A', TrackListLogic.sectionCharFor(sortedByTitle[2], SortOrder.TITLE))
        assertEquals('M', TrackListLogic.sectionCharFor(sortedByTitle[3], SortOrder.TITLE))
        assertEquals('Z', TrackListLogic.sectionCharFor(sortedByTitle[4], SortOrder.TITLE))

        assertEquals(setOf('1', '5', 'A', 'M', 'Z'), TrackListLogic.availableSymbolsFor(sortedByTitle, SortOrder.TITLE))

        // Exact matches
        assertEquals(0, TrackListLogic.findTargetIndexForSymbol(sortedByTitle, '1', SortOrder.TITLE))
        assertEquals(1, TrackListLogic.findTargetIndexForSymbol(sortedByTitle, '5', SortOrder.TITLE))
        assertEquals(2, TrackListLogic.findTargetIndexForSymbol(sortedByTitle, 'A', SortOrder.TITLE))
        assertEquals(3, TrackListLogic.findTargetIndexForSymbol(sortedByTitle, 'M', SortOrder.TITLE))
        assertEquals(4, TrackListLogic.findTargetIndexForSymbol(sortedByTitle, 'Z', SortOrder.TITLE))

        // Nearest next section when exact letter has no songs
        assertEquals(3, TrackListLogic.findTargetIndexForSymbol(sortedByTitle, 'B', SortOrder.TITLE))
        assertEquals(4, TrackListLogic.findTargetIndexForSymbol(sortedByTitle, 'N', SortOrder.TITLE))
    }
}
