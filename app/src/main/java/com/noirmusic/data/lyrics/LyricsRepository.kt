package com.noirmusic.data.lyrics

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.ceil

/**
 * Lyrics lookup policy: in-memory cache → persistent disk cache → in-flight de-duplication →
 * 429 cooldown → `/api/get` → validated `/api/search`. At most two HTTP requests per lookup, no automatic retries.
 *
 * @param scope long-lived scope used for shared in-flight requests, so a cancelled collector doesn't cancel a request
 *   another caller is waiting on.
 * @param clock monotonic milliseconds (e.g. `SystemClock.elapsedRealtime`).
 */
class LyricsRepository(
    private val api: LrclibDataSource,
    private val scope: CoroutineScope,
    private val clock: () -> Long,
    private val cacheSize: Int = 100,
    private val diskCache: LyricsDiskCache = NoOpLyricsDiskCache,
) {
    private val mutex = Mutex()
    private val cache = object : LinkedHashMap<String, LyricsResult>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, LyricsResult>?): Boolean = size > cacheSize
    }
    private val inFlight = HashMap<String, Deferred<LyricsResult>>()

    @Volatile
    private var cooldownUntilMs: Long? = null

    /** Returns a cached definitive result without any network access, or `null`. */
    fun cached(query: LyricsQuery): LyricsResult? = synchronized(cache) { cache[query.cacheKey] }

    suspend fun getLyrics(query: LyricsQuery, forceRefresh: Boolean = false): LyricsResult {
        val key = query.cacheKey
        if (!forceRefresh) {
            cached(query)?.let { return it }
            diskCache.get(key)?.let { persisted ->
                synchronized(cache) { cache[key] = persisted }
                return persisted
            }
        } else {
            synchronized(cache) { cache.remove(key) }
            diskCache.remove(key)
        }

        val deferred = mutex.withLock {
            if (!forceRefresh) {
                cached(query)?.let { return it }
            }
            inFlight[key]?.let { return@withLock it }

            val until = cooldownUntilMs
            val now = clock()
            if (until != null && now < until) {
                val remainingMs = until - now
                return LyricsResult.Failed(LyricsError.RateLimited(ceil(remainingMs / 1000.0).toLong().coerceAtLeast(1)))
            }

            scope.async {
                try {
                    val result = lookup(query)
                    if (result !is LyricsResult.Failed) {
                        synchronized(cache) { cache[key] = result }
                        if (result !is LyricsResult.NotSearchable) {
                            diskCache.put(key, query.mediaId, result)
                        }
                    }
                    result
                } finally {
                    withContext(NonCancellable) { mutex.withLock { inFlight.remove(key) } }
                }
            }.also { inFlight[key] = it }
        }
        return deferred.await()
    }

    private suspend fun lookup(query: LyricsQuery): LyricsResult {
        if (query.artist == null && query.titleFromFileName) return LyricsResult.NotSearchable

        if (query.artist != null) {
            when (val r = api.get(query.title, query.artist, query.durationSec)) {
                is ApiResult.Failure -> return fail(r.error)
                is ApiResult.Success -> {
                    val dto = r.value
                    if (dto != null && LyricsMatcher.isAcceptable(query, dto)) {
                        val result = LyricsMatcher.toResult(dto)
                        if (result != LyricsResult.NotFound) return result
                    }
                }
            }
        }

        return when (val r = api.search(TextNormalizer.cleanTitle(query.title), query.artist)) {
            is ApiResult.Failure -> fail(r.error)
            is ApiResult.Success -> LyricsMatcher.selectBest(query, r.value)
                ?.let(LyricsMatcher::toResult)
                ?: LyricsResult.NotFound
        }
    }

    private fun fail(error: LyricsError): LyricsResult {
        if (error is LyricsError.RateLimited) {
            cooldownUntilMs = clock() + error.retryAfterSec * 1000
        }
        return LyricsResult.Failed(error)
    }
}
