package com.noirmusic.data.local

import com.noirmusic.data.lyrics.LyricLine
import com.noirmusic.data.lyrics.LyricsDiskCache
import com.noirmusic.data.lyrics.LyricsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

@Serializable
private data class StoredLyricLine(val timeMs: Long, val text: String)

class RoomLyricsDiskCache(
    private val dao: LyricsCacheDao,
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val wallClockMs: () -> Long = System::currentTimeMillis,
    private val notFoundTtlMs: Long = 7L * 24 * 60 * 60 * 1000,
) : LyricsDiskCache {

    override suspend fun get(cacheKey: String): LyricsResult? = withContext(Dispatchers.IO) {
        val entity = dao.getByKey(cacheKey) ?: return@withContext null
        try {
            when (entity.kind) {
                KIND_SYNCED -> {
                    val raw = entity.payloadJson ?: return@withContext null
                    val stored = json.decodeFromString(ListSerializer(StoredLyricLine.serializer()), raw)
                    LyricsResult.Synced(
                        lines = stored.map { LyricLine(it.timeMs, it.text) },
                        plainText = entity.plainText,
                    )
                }
                KIND_PLAIN -> {
                    val raw = entity.payloadJson ?: return@withContext null
                    val lines = json.decodeFromString(ListSerializer(String.serializer()), raw)
                    LyricsResult.Plain(lines)
                }
                KIND_INSTRUMENTAL -> LyricsResult.Instrumental
                KIND_NOT_FOUND -> {
                    if (wallClockMs() - entity.updatedAtMs > notFoundTtlMs) {
                        dao.deleteByKey(cacheKey)
                        null
                    } else {
                        LyricsResult.NotFound
                    }
                }
                else -> null
            }
        } catch (_: Exception) {
            dao.deleteByKey(cacheKey)
            null
        }
    }

    override suspend fun put(cacheKey: String, mediaId: String, result: LyricsResult) {
        val entity = when (result) {
            is LyricsResult.Synced -> CachedLyricsEntity(
                cacheKey = cacheKey,
                mediaId = mediaId,
                kind = KIND_SYNCED,
                payloadJson = json.encodeToString(
                    ListSerializer(StoredLyricLine.serializer()),
                    result.lines.map { StoredLyricLine(it.timeMs, it.text) },
                ),
                plainText = result.plainText,
                updatedAtMs = wallClockMs(),
            )
            is LyricsResult.Plain -> CachedLyricsEntity(
                cacheKey = cacheKey,
                mediaId = mediaId,
                kind = KIND_PLAIN,
                payloadJson = json.encodeToString(ListSerializer(String.serializer()), result.lines),
                plainText = null,
                updatedAtMs = wallClockMs(),
            )
            LyricsResult.Instrumental -> CachedLyricsEntity(
                cacheKey = cacheKey,
                mediaId = mediaId,
                kind = KIND_INSTRUMENTAL,
                payloadJson = null,
                plainText = null,
                updatedAtMs = wallClockMs(),
            )
            LyricsResult.NotFound -> CachedLyricsEntity(
                cacheKey = cacheKey,
                mediaId = mediaId,
                kind = KIND_NOT_FOUND,
                payloadJson = null,
                plainText = null,
                updatedAtMs = wallClockMs(),
            )
            LyricsResult.NotSearchable, is LyricsResult.Failed -> return
        }
        withContext(Dispatchers.IO) {
            dao.upsert(entity)
        }
    }

    override suspend fun remove(cacheKey: String) {
        withContext(Dispatchers.IO) {
            dao.deleteByKey(cacheKey)
        }
    }

    private companion object {
        const val KIND_SYNCED = "SYNCED"
        const val KIND_PLAIN = "PLAIN"
        const val KIND_INSTRUMENTAL = "INSTRUMENTAL"
        const val KIND_NOT_FOUND = "NOT_FOUND"
    }
}
