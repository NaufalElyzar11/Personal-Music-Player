package com.noirmusic.playback

import android.content.ContentUris
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.noirmusic.data.model.Track

const val EXTRA_TITLE_FROM_FILE_NAME = "noir.titleFromFileName"

/** Controller-side item: identity + metadata only. The service resolves the URI from [MediaItem.mediaId]. */
fun Track.toMediaItem(): MediaItem {
    val extras = Bundle().apply { putBoolean(EXTRA_TITLE_FROM_FILE_NAME, titleFromFileName) }
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)
        .setDurationMs(durationMs.takeIf { it > 0 })
        .setIsPlayable(true)
        .setIsBrowsable(false)
        .setExtras(extras)
        .build()
    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setMediaMetadata(metadata)
        .build()
}

/** Service-side: rebuilds an item with a content URI derived from its numeric media ID. `null` if invalid. */
fun resolveForPlayback(item: MediaItem, collectionUri: Uri): MediaItem? {
    val id = item.mediaId.toLongOrNull() ?: return null
    return item.buildUpon()
        .setUri(ContentUris.withAppendedId(collectionUri, id))
        .build()
}
