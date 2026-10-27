package com.noirmusic.data.media

import com.noirmusic.data.lyrics.TextNormalizer
import com.noirmusic.data.model.Track
import java.text.Collator

enum class SortOrder(val label: String) {
    TITLE("Title"),
    ARTIST("Artist"),
    RECENTLY_ADDED("Recently added"),
}

/** Pure filtering, sorting, and fast-scroll indexing of the library list. */
object TrackListLogic {

    val FAST_SCROLL_SYMBOLS: List<Char> = ('0'..'9').toList() + ('A'..'Z').toList()

    fun apply(tracks: List<Track>, query: String, sort: SortOrder, collator: Collator): List<Track> {
        val needle = TextNormalizer.normalizeForSearch(query)
        val filtered = if (needle.isEmpty()) {
            tracks
        } else {
            tracks.filter { track ->
                TextNormalizer.normalizeForSearch(track.title).contains(needle) ||
                    (track.artist?.let { TextNormalizer.normalizeForSearch(it).contains(needle) } ?: false) ||
                    (track.album?.let { TextNormalizer.normalizeForSearch(it).contains(needle) } ?: false)
            }
        }
        return sortTracks(filtered, sort, collator)
    }

    fun sortTracks(tracks: List<Track>, sort: SortOrder, collator: Collator): List<Track> {
        val byTitle = Comparator<Track> { a, b -> collator.compare(a.title, b.title) }
        val comparator: Comparator<Track> = when (sort) {
            SortOrder.TITLE -> byTitle.thenBy { it.id }
            SortOrder.ARTIST -> Comparator<Track> { a, b ->
                when {
                    a.artist == null && b.artist == null -> 0
                    a.artist == null -> 1
                    b.artist == null -> -1
                    else -> collator.compare(a.artist, b.artist)
                }
            }.then(byTitle).thenBy { it.id }
            SortOrder.RECENTLY_ADDED -> compareByDescending<Track> { it.dateAddedSec }.then(byTitle).thenBy { it.id }
        }
        return tracks.sortedWith(comparator)
    }

    fun sectionCharFor(track: Track, sort: SortOrder): Char {
        val source = when (sort) {
            SortOrder.ARTIST -> track.artist?.takeIf { it.isNotBlank() } ?: track.title
            SortOrder.TITLE, SortOrder.RECENTLY_ADDED -> track.title
        }
        val normalized = TextNormalizer.normalizeForSearch(source)
        val firstAlnum = normalized.firstOrNull { it in '0'..'9' || it in 'a'..'z' }
        return when (firstAlnum) {
            null -> '0'
            in '0'..'9' -> firstAlnum
            else -> firstAlnum.uppercaseChar()
        }
    }

    fun availableSymbolsFor(tracks: List<Track>, sort: SortOrder): Set<Char> =
        tracks.mapTo(mutableSetOf()) { sectionCharFor(it, sort) }

    fun findTargetIndexForSymbol(tracks: List<Track>, symbol: Char, sort: SortOrder): Int? {
        if (tracks.isEmpty()) return null
        val target = symbol.uppercaseChar()
        val exactIndex = tracks.indexOfFirst { sectionCharFor(it, sort) == target }
        if (exactIndex >= 0) return exactIndex
        if (sort == SortOrder.RECENTLY_ADDED) return null
        val nextIndex = tracks.indexOfFirst { sectionCharFor(it, sort) > target }
        return if (nextIndex >= 0) nextIndex else tracks.lastIndex
    }
}
