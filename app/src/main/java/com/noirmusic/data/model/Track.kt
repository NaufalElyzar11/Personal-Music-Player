package com.noirmusic.data.model

/**
 * A playable audio file discovered through MediaStore.
 *
 * @property artist `null` when the file has no usable artist tag.
 * @property durationMs `0` when MediaStore does not know the duration.
 * @property titleFromFileName `true` when [title] was derived from the file name because the title tag was missing.
 */
data class Track(
    val id: Long,
    val title: String,
    val artist: String?,
    val album: String?,
    val durationMs: Long,
    val dateAddedSec: Long,
    val titleFromFileName: Boolean,
) {
    val displayArtist: String get() = artist ?: UNKNOWN_ARTIST

    companion object {
        const val UNKNOWN_ARTIST = "Unknown artist"
    }
}
