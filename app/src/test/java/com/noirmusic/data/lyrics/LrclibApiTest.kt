package com.noirmusic.data.lyrics

import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Headers
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class LrclibApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: LrclibApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val ua = HttpErrorMapping.buildUserAgent("1.0.0", "")
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", ua)
                        .header("Accept", "application/json")
                        .build(),
                )
            }
            .build()
        api = LrclibApi(client, LrclibJson, server.url("/"))
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun getEncodesQueryParametersSendsHeadersAndParses200() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .body(
                    """{"id":42,"trackName":"Rock & Roll/Ünïcode ?#","artistName":"Led / Zep","duration":220.0,"instrumental":false,"plainLyrics":null,"syncedLyrics":"[00:01.00]Hey","extraField":"ignored"}""",
                )
                .build(),
        )

        val result = api.get("Rock & Roll/Ünïcode ?#", "Led / Zep", null)
        val req = server.takeRequest()
        assertEquals("/api/get", req.url.encodedPath)
        assertEquals("Rock & Roll/Ünïcode ?#", req.url.queryParameter("track_name"))
        assertEquals("Led / Zep", req.url.queryParameter("artist_name"))
        assertNull(req.url.queryParameter("duration"))
        assertTrue(req.headers["User-Agent"].orEmpty().startsWith("Noir Music v"))
        assertEquals("application/json", req.headers["Accept"])

        val dto = (result as ApiResult.Success).value!!
        assertEquals(42L, dto.id)
        assertNull(dto.plainLyrics)
        assertEquals("[00:01.00]Hey", dto.syncedLyrics)
    }

    @Test
    fun get404ReturnsSuccessNullAndSearchParsesList() = runTest {
        server.enqueue(MockResponse.Builder().code(404).body("""{"statusCode":404}""").build())
        assertEquals(ApiResult.Success(null), api.get("Song", "Artist", 180))
        val getReq = server.takeRequest()
        assertEquals("180", getReq.url.queryParameter("duration"))

        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .body("""[{"id":7,"trackName":"Song","artistName":"Artist","plainLyrics":"Words"}]""")
                .build(),
        )
        val search = api.search("Song", "Artist") as ApiResult.Success
        assertEquals(1, search.value.size)
        assertEquals(7L, search.value.first().id)
    }

    @Test
    fun maps429WithAndWithoutHeader503400AndMalformedPayloads() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .code(429)
                .headers(Headers.headersOf("Retry-After", "120"))
                .body("{}")
                .build(),
        )
        assertEquals(ApiResult.Failure(LyricsError.RateLimited(120)), api.get("A", "B", 100))

        server.enqueue(MockResponse.Builder().code(429).body("{}").build())
        assertEquals(ApiResult.Failure(LyricsError.RateLimited(60)), api.get("A", "B", 100))

        server.enqueue(MockResponse.Builder().code(503).body("err").build())
        assertEquals(ApiResult.Failure(LyricsError.Server(503)), api.get("A", "B", 100))

        server.enqueue(MockResponse.Builder().code(400).body("bad").build())
        assertEquals(ApiResult.Failure(LyricsError.Http(400)), api.get("A", "B", 100))

        server.enqueue(MockResponse.Builder().code(200).body("not-json").build())
        assertEquals(ApiResult.Failure(LyricsError.Malformed), api.get("A", "B", 100))

        // Object returned where array is expected on /api/search
        server.enqueue(MockResponse.Builder().code(200).body("""{"id":1}""").build())
        assertEquals(ApiResult.Failure(LyricsError.Malformed), api.search("A", "B"))
    }

    @Test
    fun mapsSlowResponseToTimeout() = runTest {
        val fastTimeoutClient = OkHttpClient.Builder()
            .readTimeout(150, TimeUnit.MILLISECONDS)
            .callTimeout(300, TimeUnit.MILLISECONDS)
            .build()
        val shortApi = LrclibApi(fastTimeoutClient, LrclibJson, server.url("/"))

        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .headersDelay(600, TimeUnit.MILLISECONDS)
                .body("{}")
                .build(),
        )
        assertEquals(ApiResult.Failure(LyricsError.Timeout), shortApi.get("A", "B", 100))
    }
}
