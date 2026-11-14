package com.noirmusic.data.lyrics

/** Disk persistence contract for resolved lyrics lookups. */
interface LyricsDiskCache {
    suspend fun get(cacheKey: String): LyricsResult?
    suspend fun put(cacheKey: String, mediaId: String, result: LyricsResult)
    suspend fun remove(cacheKey: String)
}

object NoOpLyricsDiskCache : LyricsDiskCache {
    override suspend fun get(cacheKey: String): LyricsResult? = null
    override suspend fun put(cacheKey: String, mediaId: String, result: LyricsResult) = Unit
    override suspend fun remove(cacheKey: String) = Unit
}
