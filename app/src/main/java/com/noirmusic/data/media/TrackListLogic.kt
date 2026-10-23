package com.noirmusic.data.media

import com.noirmusic.data.lyrics.TextNormalizer
import com.noirmusic.data.model.Track
import java.text.Collator

enum class SortOrder(val label: String) {
    TITLE("Title"),
    ARTIST("Artist"),
    RECENTLY_ADDED("Recently added"),
}

/** Pure filtering and sorting of the library list. */
object TrackListLogic {

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
        return filtered.sortedWith(comparator)
    }
}
