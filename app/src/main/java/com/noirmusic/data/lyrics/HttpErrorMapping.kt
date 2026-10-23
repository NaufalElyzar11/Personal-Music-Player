package com.noirmusic.data.lyrics

import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

object HttpErrorMapping {

    const val DEFAULT_RETRY_AFTER_SEC = 60L
    private const val MIN_RETRY_AFTER_SEC = 1L
    private const val MAX_RETRY_AFTER_SEC = 3600L

    fun mapIoException(e: IOException): LyricsError = when (e) {
        is UnknownHostException, is ConnectException, is NoRouteToHostException -> LyricsError.Offline
        is SocketTimeoutException -> LyricsError.Timeout
        is InterruptedIOException -> LyricsError.Timeout // OkHttp call timeout
        else -> LyricsError.Network
    }

    /** Parses `Retry-After` (delta-seconds or HTTP-date). Missing/invalid → 60 s. Result clamped to [1, 3600]. */
    fun parseRetryAfterSeconds(header: String?, nowEpochMs: Long): Long {
        val value = header?.trim().orEmpty()
        val seconds: Long = value.toLongOrNull()?.takeIf { it >= 0 }
            ?: runCatching {
                val date = ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)
                ceil((date.toInstant().toEpochMilli() - nowEpochMs) / 1000.0).toLong()
            }.getOrNull()
            ?: DEFAULT_RETRY_AFTER_SEC
        return seconds.coerceIn(MIN_RETRY_AFTER_SEC, MAX_RETRY_AFTER_SEC)
    }

    /** User-Agent following LRCLIB's client identification guidance. */
    fun buildUserAgent(versionName: String, contact: String): String =
        "Noir Music v$versionName (" + contact.trim().ifBlank { "personal non-commercial Android player" } + ")"
}
