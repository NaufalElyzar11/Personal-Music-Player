package com.noirmusic.data.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

interface TrackStatsRepository {
    val favoriteIds: Flow<Set<Long>>
    val recentStats: Flow<List<TrackStatsEntity>>
    val mostPlayedStats: Flow<List<TrackStatsEntity>>
    suspend fun toggleFavorite(trackId: Long)
    suspend fun recordPlay(trackId: Long)
}

class RoomTrackStatsRepository(
    private val dao: TrackStatsDao,
    private val wallClockMs: () -> Long = System::currentTimeMillis,
) : TrackStatsRepository {

    private val mutex = Mutex()

    override val favoriteIds: Flow<Set<Long>> = dao.observeFavoriteIds().map { it.toSet() }
    override val recentStats: Flow<List<TrackStatsEntity>> = dao.observeRecent()
    override val mostPlayedStats: Flow<List<TrackStatsEntity>> = dao.observeMostPlayed()

    override suspend fun toggleFavorite(trackId: Long) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val current = dao.getById(trackId) ?: TrackStatsEntity(trackId = trackId)
                val nextFav = !current.isFavorite
                dao.upsert(
                    current.copy(
                        isFavorite = nextFav,
                        favoritedAtMs = if (nextFav) wallClockMs() else null,
                    ),
                )
            }
        }
    }

    override suspend fun recordPlay(trackId: Long) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val current = dao.getById(trackId) ?: TrackStatsEntity(trackId = trackId)
                dao.upsert(
                    current.copy(
                        playCount = current.playCount + 1,
                        lastPlayedAtMs = wallClockMs(),
                    ),
                )
            }
        }
    }
}
