package com.noirmusic.data.playlist

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

interface PlaylistRepository {
    val playlists: StateFlow<List<Playlist>>
    suspend fun createPlaylist(name: String, initialTrackId: Long? = null): Playlist
    suspend fun deletePlaylist(playlistId: String)
    suspend fun renamePlaylist(playlistId: String, newName: String)
    suspend fun addTrackToPlaylist(playlistId: String, trackId: Long): Boolean
    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: Long)
    suspend fun reorderTracks(playlistId: String, newTrackIds: List<Long>)
}

class JsonPlaylistRepository(
    context: Context,
    private val json: Json = Json { ignoreUnknownKeys = true; prettyPrint = true },
    fileName: String = "playlists.json",
) : PlaylistRepository {

    private val file = File(context.filesDir, fileName)
    private val mutex = Mutex()
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    override val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    init {
        loadInitial()
    }

    private fun loadInitial() {
        try {
            if (file.exists()) {
                val text = file.readText()
                if (text.isNotBlank()) {
                    _playlists.value = json.decodeFromString(ListSerializer(Playlist.serializer()), text)
                }
            }
        } catch (_: Exception) {
            _playlists.value = emptyList()
        }
    }

    private suspend fun persistLocked() = withContext(Dispatchers.IO) {
        try {
            val parent = file.parentFile ?: return@withContext
            if (!parent.exists()) parent.mkdirs()
            val temp = File(parent, "${file.name}.tmp")
            val data = json.encodeToString(ListSerializer(Playlist.serializer()), _playlists.value)
            temp.writeText(data)
            if (temp.exists()) {
                if (file.exists()) file.delete()
                temp.renameTo(file)
            }
        } catch (_: Exception) {
            // Best effort write
        }
    }

    override suspend fun createPlaylist(name: String, initialTrackId: Long?): Playlist = mutex.withLock {
        val trimmed = name.trim().ifBlank { "Playlist ${_playlists.value.size + 1}" }
        val tracks = if (initialTrackId != null) listOf(initialTrackId) else emptyList()
        val newPlaylist = Playlist(name = trimmed, trackIds = tracks)
        _playlists.value = _playlists.value + newPlaylist
        persistLocked()
        newPlaylist
    }

    override suspend fun deletePlaylist(playlistId: String): Unit = mutex.withLock {
        _playlists.value = _playlists.value.filterNot { it.id == playlistId }
        persistLocked()
    }

    override suspend fun renamePlaylist(playlistId: String, newName: String): Unit = mutex.withLock {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return@withLock
        _playlists.value = _playlists.value.map {
            if (it.id == playlistId) it.copy(name = trimmed) else it
        }
        persistLocked()
    }

    override suspend fun addTrackToPlaylist(playlistId: String, trackId: Long): Boolean = mutex.withLock {
        var added = false
        _playlists.value = _playlists.value.map { playlist ->
            if (playlist.id == playlistId) {
                if (!playlist.trackIds.contains(trackId)) {
                    added = true
                    playlist.copy(trackIds = playlist.trackIds + trackId)
                } else playlist
            } else playlist
        }
        if (added) persistLocked()
        added
    }

    override suspend fun removeTrackFromPlaylist(playlistId: String, trackId: Long): Unit = mutex.withLock {
        _playlists.value = _playlists.value.map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(trackIds = playlist.trackIds.filterNot { it == trackId })
            } else playlist
        }
        persistLocked()
    }

    override suspend fun reorderTracks(playlistId: String, newTrackIds: List<Long>): Unit = mutex.withLock {
        _playlists.value = _playlists.value.map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(trackIds = newTrackIds)
            } else playlist
        }
        persistLocked()
    }
}
