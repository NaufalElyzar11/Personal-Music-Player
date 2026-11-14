package com.noirmusic.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.playbackDataStore: DataStore<Preferences> by preferencesDataStore(name = "noir_playback_prefs")

data class PlaybackSnapshot(
    val queueTrackIds: List<Long>,
    val currentIndex: Int,
    val positionMs: Long,
    val shuffleEnabled: Boolean,
    val repeatMode: Int,
)

interface PlaybackStateStore {
    val snapshot: Flow<PlaybackSnapshot?>
    suspend fun readLatest(): PlaybackSnapshot?
    suspend fun save(snapshot: PlaybackSnapshot)
    suspend fun clear()
}

class DataStorePlaybackPreferencesStore(context: Context) : PlaybackStateStore {

    private val dataStore = context.applicationContext.playbackDataStore

    override val snapshot: Flow<PlaybackSnapshot?> = dataStore.data.map(::decode)

    override suspend fun readLatest(): PlaybackSnapshot? = snapshot.first()

    override suspend fun save(snapshot: PlaybackSnapshot) {
        dataStore.edit { prefs ->
            if (snapshot.queueTrackIds.isEmpty()) {
                prefs.remove(KEY_QUEUE_IDS)
                prefs.remove(KEY_CURRENT_INDEX)
                prefs.remove(KEY_POSITION_MS)
            } else {
                prefs[KEY_QUEUE_IDS] = snapshot.queueTrackIds.joinToString(",")
                prefs[KEY_CURRENT_INDEX] = snapshot.currentIndex
                prefs[KEY_POSITION_MS] = snapshot.positionMs.coerceAtLeast(0L)
            }
            prefs[KEY_SHUFFLE_ENABLED] = snapshot.shuffleEnabled
            prefs[KEY_REPEAT_MODE] = snapshot.repeatMode
        }
    }

    override suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_QUEUE_IDS)
            prefs.remove(KEY_CURRENT_INDEX)
            prefs.remove(KEY_POSITION_MS)
        }
    }

    private fun decode(prefs: Preferences): PlaybackSnapshot? {
        val rawIds = prefs[KEY_QUEUE_IDS].orEmpty()
        val ids = if (rawIds.isBlank()) {
            emptyList()
        } else {
            rawIds.split(',').mapNotNull { it.trim().toLongOrNull() }
        }
        val shuffle = prefs[KEY_SHUFFLE_ENABLED] ?: false
        val repeat = prefs[KEY_REPEAT_MODE] ?: 0
        if (ids.isEmpty() && !shuffle && repeat == 0) return null
        return PlaybackSnapshot(
            queueTrackIds = ids,
            currentIndex = prefs[KEY_CURRENT_INDEX] ?: 0,
            positionMs = (prefs[KEY_POSITION_MS] ?: 0L).coerceAtLeast(0L),
            shuffleEnabled = shuffle,
            repeatMode = repeat,
        )
    }

    private companion object {
        val KEY_QUEUE_IDS = stringPreferencesKey("queue_track_ids")
        val KEY_CURRENT_INDEX = intPreferencesKey("current_index")
        val KEY_POSITION_MS = longPreferencesKey("position_ms")
        val KEY_SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
        val KEY_REPEAT_MODE = intPreferencesKey("repeat_mode")
    }
}
