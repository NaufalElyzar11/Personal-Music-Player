package com.noirmusic.data.lyrics

import kotlinx.serialization.Serializable

/** One timed lyric line. [text] may be empty, marking an instrumental gap. */
data class LyricLine(val timeMs: Long, val text: String)

/** Everything needed to look up lyrics for one track. */
data class LyricsQuery(
    val mediaId: String,
    val title: String,
    /** `null` when the artist is unknown. */
    val artist: String?,
    val album: String?,
    /** Rounded seconds, `null` if unknown or outside 1..3600. */
    val durationSec: Int?,
    val titleFromFileName: Boolean,
) {
    val cacheKey: String
        get() = listOf(
            mediaId,
            TextNormalizer.normalize(title),
            artist?.let(TextNormalizer::normalize).orEmpty(),
            durationSec?.toString().orEmpty(),
        ).joinToString("|")

    companion object {
        fun durationSecFromMs(durationMs: Long): Int? {
            if (durationMs <= 0) return null
            val sec = Math.round(durationMs / 1000.0)
            return if (sec in 1..3600) sec.toInt() else null
        }
    }
}

sealed interface LyricsError {
    data object Offline : LyricsError
    data object Timeout : LyricsError
    data class RateLimited(val retryAfterSec: Long) : LyricsError
    data class Server(val code: Int) : LyricsError
    data class Http(val code: Int) : LyricsError
    data object Malformed : LyricsError
    data object Network : LyricsError
}

sealed interface LyricsResult {
    data class Synced(val lines: List<LyricLine>, val plainText: String?) : LyricsResult
    data class Plain(val lines: List<String>) : LyricsResult
    data object Instrumental : LyricsResult
    data object NotFound : LyricsResult
    data object NotSearchable : LyricsResult
    data class Failed(val error: LyricsError) : LyricsResult
}

sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Failure(val error: LyricsError) : ApiResult<Nothing>
}

@Serializable
data class LrclibRecordDto(
    val id: Long? = null,
    val trackName: String? = null,
    val name: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    val duration: Double? = null,
    val instrumental: Boolean = false,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null,
) {
    val resolvedTrackName: String?
        get() = trackName ?: name
}
