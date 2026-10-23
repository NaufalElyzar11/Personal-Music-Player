package com.noirmusic.data.lyrics

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class HttpErrorMappingTest {

    @Test
    fun mapsIoExceptionsToSpecificErrors() {
        assertEquals(LyricsError.Offline, HttpErrorMapping.mapIoException(UnknownHostException()))
        assertEquals(LyricsError.Offline, HttpErrorMapping.mapIoException(ConnectException()))
        assertEquals(LyricsError.Offline, HttpErrorMapping.mapIoException(NoRouteToHostException()))
        assertEquals(LyricsError.Timeout, HttpErrorMapping.mapIoException(SocketTimeoutException()))
        assertEquals(LyricsError.Timeout, HttpErrorMapping.mapIoException(InterruptedIOException()))
        assertEquals(LyricsError.Network, HttpErrorMapping.mapIoException(IOException("boom")))
    }

    @Test
    fun parsesRetryAfterIntegerHttpDateFallbackAndClamping() {
        val now = 1_700_000_000_000L
        assertEquals(120L, HttpErrorMapping.parseRetryAfterSeconds("120", now))
        assertEquals(1L, HttpErrorMapping.parseRetryAfterSeconds("0", now))
        assertEquals(3600L, HttpErrorMapping.parseRetryAfterSeconds("99999", now))
        assertEquals(60L, HttpErrorMapping.parseRetryAfterSeconds(null, now))
        assertEquals(60L, HttpErrorMapping.parseRetryAfterSeconds("not-a-date", now))

        val futureDate = ZonedDateTime.ofInstant(Instant.ofEpochMilli(now + 45_000), ZoneOffset.UTC)
            .format(DateTimeFormatter.RFC_1123_DATE_TIME)
        assertEquals(45L, HttpErrorMapping.parseRetryAfterSeconds(futureDate, now))
    }

    @Test
    fun buildsCompliantUserAgent() {
        assertEquals(
            "Noir Music v1.0.0 (personal non-commercial Android player)",
            HttpErrorMapping.buildUserAgent("1.0.0", ""),
        )
        assertEquals(
            "Noir Music v1.0.0 (https://example.invalid)",
            HttpErrorMapping.buildUserAgent("1.0.0", "https://example.invalid"),
        )
    }
}
