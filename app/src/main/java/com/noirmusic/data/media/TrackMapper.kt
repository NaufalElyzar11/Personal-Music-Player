package com.noirmusic.data.media

import com.noirmusic.data.lyrics.TextNormalizer
import com.noirmusic.data.model.Track

/** Pure MediaStore row → [Track] mapping with metadata fallbacks. */
object TrackMapper {

    const val UNKNOWN_TITLE = "Unknown title"

    fun fromRow(
        id: Long,
        title: String?,
        artist: String?,
        album: String?,
        durationMs: Long?,
        dateAddedSec: Long?,
        displayName: String?,
    ): Track {
        val cleanTitle = title?.trim().orEmpty()
        val resolvedTitle: String
        val fromFileName: Boolean
        if (cleanTitle.isNotEmpty()) {
            resolvedTitle = cleanTitle
            fromFileName = false
        } else {
            val fileTitle = displayName?.trim().orEmpty().let(::stripExtension).trim()
            resolvedTitle = fileTitle.ifEmpty { UNKNOWN_TITLE }
            fromFileName = true
        }

        val cleanArtist = artist?.trim()?.takeIf { it.isNotEmpty() && !TextNormalizer.isUnknownArtist(it) }
        val cleanAlbum = album?.trim()?.takeIf { it.isNotEmpty() && !it.equals("<unknown>", ignoreCase = true) }

        return Track(
            id = id,
            title = resolvedTitle,
            artist = cleanArtist,
            album = cleanAlbum,
            durationMs = (durationMs ?: 0L).coerceAtLeast(0L),
            dateAddedSec = dateAddedSec ?: 0L,
            titleFromFileName = fromFileName,
        )
    }

    private fun stripExtension(name: String): String {
        val dot = name.lastIndexOf('.')
        return if (dot > 0) name.substring(0, dot) else name
    }
}
