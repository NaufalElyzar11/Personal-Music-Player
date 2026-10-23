package com.noirmusic.data.media

import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.noirmusic.data.model.Track
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface TrackRepository {
    /** Loads all music tracks. Throws on failure (e.g. [SecurityException]). */
    suspend fun loadTracks(): List<Track>
}

class MediaStoreTrackRepository(
    private val contentResolver: ContentResolver,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : TrackRepository {

    override suspend fun loadTracks(): List<Track> = withContext(ioDispatcher) {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DISPLAY_NAME,
        )
        val selection = "(${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ${MediaStore.Audio.Media.MIME_TYPE} LIKE 'audio/%') AND (${MediaStore.Audio.Media.DURATION} IS NULL OR ${MediaStore.Audio.Media.DURATION} >= 1000)"
        val cursor = contentResolver.query(audioCollectionUri(), projection, selection, null, null)
            ?: throw IllegalStateException("MediaStore returned no cursor")
        cursor.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val result = ArrayList<Track>(c.count)
            while (c.moveToNext()) {
                result += TrackMapper.fromRow(
                    id = c.getLong(idCol),
                    title = c.getStringOrNull(titleCol),
                    artist = c.getStringOrNull(artistCol),
                    album = c.getStringOrNull(albumCol),
                    durationMs = if (c.isNull(durationCol)) null else c.getLong(durationCol),
                    dateAddedSec = if (c.isNull(dateCol)) null else c.getLong(dateCol),
                    displayName = c.getStringOrNull(nameCol),
                )
            }
            result
        }
    }

    private fun android.database.Cursor.getStringOrNull(index: Int): String? =
        if (isNull(index)) null else getString(index)

    companion object {
        fun audioCollectionUri(): Uri =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
    }
}
