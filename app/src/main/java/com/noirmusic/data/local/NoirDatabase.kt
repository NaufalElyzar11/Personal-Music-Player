package com.noirmusic.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CachedLyricsEntity::class, TrackStatsEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class NoirDatabase : RoomDatabase() {
    abstract fun lyricsCacheDao(): LyricsCacheDao
    abstract fun trackStatsDao(): TrackStatsDao

    companion object {
        fun create(context: Context): NoirDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                NoirDatabase::class.java,
                "noir_music.db",
            ).build()
    }
}
