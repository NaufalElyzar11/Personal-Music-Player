package com.noirmusic.data.lyrics

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeLrclibDataSource : LrclibDataSource {
    var getCalls = 0
    var searchCalls = 0
    var lastSearchTrack: String? = null
    var lastSearchArtist: String? = null

    var onGet: suspend (String, String, Int?) -> ApiResult<LrclibRecordDto?> = { _, _, _ -> ApiResult.Success(null) }
    var onSearch: suspend (String, String?) -> ApiResult<List<LrclibRecordDto>> = { _, _ -> ApiResult.Success(emptyList()) }

    override suspend fun get(trackName: String, artistName: String, durationSec: Int?): ApiResult<LrclibRecordDto?> {
        getCalls++
        return onGet(trackName, artistName, durationSec)
    }

    override suspend fun search(trackName: String, artistName: String?): ApiResult<List<LrclibRecordDto>> {
        searchCalls++
        lastSearchTrack = trackName
        lastSearchArtist = artistName
        return onSearch(trackName, artistName)
    }
}

class LyricsRepositoryTest {

    private fun query(
        id: String = "1",
        title: String = "The Chain (2004 Remaster)",
        artist: String? = "Fleetwood Mac",
        durationSec: Int? = 270,
        fromFileName: Boolean = false,
    ) = LyricsQuery(
        mediaId = id,
        title = title,
        artist = artist,
        album = "Rumours",
        durationSec = durationSec,
        titleFromFileName = fromFileName,
    )

    private fun validSyncedDto(id: Long = 10) = LrclibRecordDto(
        id = id,
        trackName = "The Chain",
        artistName = "Fleetwood Mac",
        duration = 270.0,
        syncedLyrics = "[00:05.00]Listen to the wind blow",
        plainLyrics = "Listen to the wind blow",
    )

    @Test
    fun getHitReturnsSyncedWithoutCallingSearchAndCachesResult() = runTest {
        val fake = FakeLrclibDataSource().apply {
            onGet = { _, _, _ -> ApiResult.Success(validSyncedDto()) }
        }
        val repo = LyricsRepository(fake, backgroundScope, clock = { 0L })

        val r1 = repo.getLyrics(query())
        assertTrue(r1 is LyricsResult.Synced)
        assertEquals(1, fake.getCalls)
        assertEquals(0, fake.searchCalls)

        // Second call is served from cache
        val r2 = repo.getLyrics(query())
        assertEquals(r1, r2)
        assertEquals(1, fake.getCalls)
    }

    @Test
    fun fallsBackToSearchOn404OrUnacceptableGetAndCleansTitle() = runTest {
        val fake = FakeLrclibDataSource().apply {
            onGet = { _, _, _ ->
                ApiResult.Success(validSyncedDto().copy(artistName = "Wrong Artist"))
            }
            onSearch = { _, _ -> ApiResult.Success(listOf(validSyncedDto(99))) }
        }
        val repo = LyricsRepository(fake, backgroundScope, clock = { 0L })

        val r = repo.getLyrics(query())
        assertTrue(r is LyricsResult.Synced)
        assertEquals(1, fake.getCalls)
        assertEquals(1, fake.searchCalls)
        assertEquals("The Chain", fake.lastSearchTrack)
    }

    @Test
    fun networkFailureOnGetDoesNotProceedToSearchAndIsNotCached() = runTest {
        var shouldFail = true
        val fake = FakeLrclibDataSource().apply {
            onGet = { _, _, _ ->
                if (shouldFail) ApiResult.Failure(LyricsError.Offline) else ApiResult.Success(validSyncedDto())
            }
        }
        val repo = LyricsRepository(fake, backgroundScope, clock = { 0L })

        assertEquals(LyricsResult.Failed(LyricsError.Offline), repo.getLyrics(query()))
        assertEquals(1, fake.getCalls)
        assertEquals(0, fake.searchCalls)

        shouldFail = false
        assertTrue(repo.getLyrics(query()) is LyricsResult.Synced)
        assertEquals(2, fake.getCalls)
    }

    @Test
    fun rateLimitCooldownBlocksFurtherNetworkCallsUntilClockAdvances() = runTest {
        var nowMs = 1_000L
        val fake = FakeLrclibDataSource().apply {
            onGet = { _, _, _ -> ApiResult.Failure(LyricsError.RateLimited(30)) }
        }
        val repo = LyricsRepository(fake, backgroundScope, clock = { nowMs })

        assertEquals(LyricsResult.Failed(LyricsError.RateLimited(30)), repo.getLyrics(query("1")))
        assertEquals(1, fake.getCalls)

        nowMs = 11_000L // 10 s elapsed, 20 s remaining
        val blocked = repo.getLyrics(query("2", title = "Another Song"))
        assertEquals(LyricsResult.Failed(LyricsError.RateLimited(20)), blocked)
        assertEquals(1, fake.getCalls) // no network call during cooldown

        nowMs = 32_000L // past cooldown
        repo.getLyrics(query("2", title = "Another Song"))
        assertEquals(2, fake.getCalls)
    }

    @Test
    fun deduplicatesConcurrentInFlightRequestsForSameTrack() = runTest {
        val gate = CompletableDeferred<Unit>()
        val fake = FakeLrclibDataSource().apply {
            onGet = { _, _, _ ->
                gate.await()
                ApiResult.Success(validSyncedDto())
            }
        }
        val repo = LyricsRepository(fake, backgroundScope, clock = { 0L })

        val a = async { repo.getLyrics(query()) }
        val b = async { repo.getLyrics(query()) }
        testScheduler.runCurrent()
        gate.complete(Unit)

        assertEquals(a.await(), b.await())
        assertEquals(1, fake.getCalls)
    }

    @Test
    fun handlesUnknownArtistNotSearchableInstrumentalPlainFallbackAndNotFoundCaching() = runTest {
        val fake = FakeLrclibDataSource()
        val repo = LyricsRepository(fake, backgroundScope, clock = { 0L })

        // Filename title + unknown artist → NotSearchable with zero network calls
        val notSearchable = repo.getLyrics(query(id = "ns", artist = null, fromFileName = true))
        assertEquals(LyricsResult.NotSearchable, notSearchable)
        assertEquals(0, fake.getCalls + fake.searchCalls)

        // Unknown artist with real title tag → skips /api/get, calls /api/search with track_name only
        fake.onSearch = { _, _ ->
            ApiResult.Success(
                listOf(
                    LrclibRecordDto(
                        id = 1,
                        trackName = "The Chain",
                        artistName = "Fleetwood Mac",
                        duration = 270.0,
                        instrumental = true,
                    ),
                ),
            )
        }
        val inst = repo.getLyrics(query(id = "ua", title = "The Chain", artist = null, durationSec = 270))
        assertEquals(LyricsResult.Instrumental, inst)
        assertEquals(0, fake.getCalls)
        assertEquals(1, fake.searchCalls)
        assertEquals(null, fake.lastSearchArtist)

        // Unparseable synced + valid plain → Plain fallback
        fake.onGet = { _, _, _ ->
            ApiResult.Success(
                validSyncedDto().copy(syncedLyrics = "[invalid]", plainLyrics = "Line 1\nLine 2"),
            )
        }
        val plain = repo.getLyrics(query(id = "pl"))
        assertEquals(LyricsResult.Plain(listOf("Line 1", "Line 2")), plain)

        // NotFound is cached
        fake.onGet = { _, _, _ -> ApiResult.Success(null) }
        fake.onSearch = { _, _ -> ApiResult.Success(emptyList()) }
        val beforeCalls = fake.getCalls + fake.searchCalls
        assertEquals(LyricsResult.NotFound, repo.getLyrics(query(id = "nf")))
        assertEquals(beforeCalls + 2, fake.getCalls + fake.searchCalls)
        assertEquals(LyricsResult.NotFound, repo.getLyrics(query(id = "nf")))
        assertEquals(beforeCalls + 2, fake.getCalls + fake.searchCalls)
    }
}
