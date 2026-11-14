package com.noirmusic.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cached_lyrics")
data class CachedLyricsEntity(
    @PrimaryKey val cacheKey: String,
    val mediaId: String,
    val kind: String,
    val payloadJson: String?,
    val plainText: String?,
    val updatedAtMs: Long,
)

@Entity(tableName = "track_stats")
data class TrackStatsEntity(
    @PrimaryKey val trackId: Long,
    val isFavorite: Boolean = false,
    val favoritedAtMs: Long? = null,
    val playCount: Int = 0,
    val lastPlayedAtMs: Long? = null,
)

@Dao
interface LyricsCacheDao {
    @Query("SELECT * FROM cached_lyrics WHERE cacheKey = :cacheKey LIMIT 1")
    suspend fun getByKey(cacheKey: String): CachedLyricsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CachedLyricsEntity)

    @Query("DELETE FROM cached_lyrics WHERE cacheKey = :cacheKey")
    suspend fun deleteByKey(cacheKey: String)
}

@Dao
interface TrackStatsDao {
    @Query("SELECT trackId FROM track_stats WHERE isFavorite = 1 ORDER BY favoritedAtMs DESC")
    fun observeFavoriteIds(): Flow<List<Long>>

    @Query("SELECT * FROM track_stats WHERE lastPlayedAtMs IS NOT NULL ORDER BY lastPlayedAtMs DESC LIMIT :limit")
    fun observeRecent(limit: Int = 30): Flow<List<TrackStatsEntity>>

    @Query("SELECT * FROM track_stats WHERE playCount > 0 ORDER BY playCount DESC, lastPlayedAtMs DESC LIMIT :limit")
    fun observeMostPlayed(limit: Int = 30): Flow<List<TrackStatsEntity>>

    @Query("SELECT * FROM track_stats WHERE trackId = :trackId LIMIT 1")
    suspend fun getById(trackId: Long): TrackStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TrackStatsEntity)
}
