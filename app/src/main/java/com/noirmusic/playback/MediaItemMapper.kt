package com.noirmusic.playback

import android.content.ContentUris
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.noirmusic.data.media.MediaStoreTrackRepository
import com.noirmusic.data.model.Track

const val EXTRA_TITLE_FROM_FILE_NAME = "noir.titleFromFileName"

/** Converts [Track] to [MediaItem] with both identity metadata and content URI. */
fun Track.toMediaItem(): MediaItem {
    val contentUri = ContentUris.withAppendedId(MediaStoreTrackRepository.audioCollectionUri(), id)
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
        .setUri(contentUri)
        .setRequestMetadata(
            MediaItem.RequestMetadata.Builder()
                .setMediaUri(contentUri)
                .build(),
        )
        .setMediaMetadata(metadata)
        .build()
}

/** Service-side: ensures the item has an actionable content URI. `null` if invalid. */
fun resolveForPlayback(item: MediaItem, collectionUri: Uri): MediaItem? {
    val id = item.mediaId.toLongOrNull()
    val uri = item.requestMetadata.mediaUri
        ?: (id?.let { ContentUris.withAppendedId(collectionUri, it) })
        ?: item.localConfiguration?.uri
        ?: return null
    return item.buildUpon()
        .setUri(uri)
        .setRequestMetadata(
            MediaItem.RequestMetadata.Builder()
                .setMediaUri(uri)
                .build(),
        )
        .build()
}
