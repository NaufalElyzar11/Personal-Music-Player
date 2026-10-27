package com.noirmusic.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noirmusic.data.media.SortOrder
import com.noirmusic.data.media.TrackListLogic
import com.noirmusic.data.media.TrackRepository
import com.noirmusic.data.model.Track
import com.noirmusic.data.playlist.Playlist
import com.noirmusic.data.playlist.PlaylistRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.Collator

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data object Empty : LibraryUiState
    data class Error(val message: String) : LibraryUiState
    data class Content(
        val all: List<Track>,
        val allSorted: List<Track>,
        val visible: List<Track>,
        val query: String,
        val sort: SortOrder,
    ) : LibraryUiState
}

private sealed interface ScanState {
    data object Idle : ScanState
    data object Loading : ScanState
    data class Loaded(val tracks: List<Track>) : ScanState
    data class Failed(val message: String) : ScanState
}

class LibraryViewModel(
    private val repository: TrackRepository,
    private val playlistRepository: PlaylistRepository,
) : ViewModel() {

    private val scan = MutableStateFlow<ScanState>(ScanState.Idle)
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    private val _sort = MutableStateFlow(SortOrder.TITLE)
    val sort: StateFlow<SortOrder> = _sort.asStateFlow()
    private var scanJob: Job? = null

    val playlists: StateFlow<List<Playlist>> = playlistRepository.playlists

    private val _selectedPlaylistId = MutableStateFlow<String?>(null)
    val selectedPlaylistId: StateFlow<String?> = _selectedPlaylistId.asStateFlow()

    val selectedPlaylistWithTracks: StateFlow<Pair<Playlist, List<Track>>?> = combine(
        playlistRepository.playlists,
        _selectedPlaylistId,
        scan,
    ) { playlistsList, selectedId, s ->
        if (selectedId == null) return@combine null
        val playlist = playlistsList.find { it.id == selectedId } ?: return@combine null
        val allTracks = (s as? ScanState.Loaded)?.tracks ?: emptyList()
        val trackMap = allTracks.associateBy { it.id }
        val resolvedTracks = playlist.trackIds.mapNotNull { trackMap[it] }
        playlist to resolvedTracks
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val uiState: StateFlow<LibraryUiState> = combine(scan, _query, _sort) { s, q, sort ->
        when (s) {
            ScanState.Idle, ScanState.Loading -> LibraryUiState.Loading
            is ScanState.Failed -> LibraryUiState.Error(s.message)
            is ScanState.Loaded ->
                if (s.tracks.isEmpty()) {
                    LibraryUiState.Empty
                } else {
                    val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }
                    val sortedAll = TrackListLogic.sortTracks(s.tracks, sort, collator)
                    val visibleTracks = if (q.isBlank()) {
                        sortedAll
                    } else {
                        TrackListLogic.apply(sortedAll, q, sort, collator)
                    }
                    LibraryUiState.Content(
                        all = s.tracks,
                        allSorted = sortedAll,
                        visible = visibleTracks,
                        query = q,
                        sort = sort,
                    )
                }
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState.Loading)

    /** Scans once after permission is granted; later calls are no-ops unless [force] (Refresh). */
    fun scan(force: Boolean = false) {
        if (!force && scan.value != ScanState.Idle) return
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            scan.value = ScanState.Loading
            scan.value = try {
                ScanState.Loaded(repository.loadTracks())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ScanState.Failed(e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun setQuery(value: String) {
        _query.value = value
    }

    fun setSort(value: SortOrder) {
        _sort.value = value
    }

    fun selectPlaylist(id: String?) {
        _selectedPlaylistId.value = id
    }

    fun createPlaylist(name: String, initialTrackId: Long? = null, onCreated: ((Playlist) -> Unit)? = null) {
        viewModelScope.launch {
            val p = playlistRepository.createPlaylist(name, initialTrackId)
            onCreated?.invoke(p)
        }
    }

    fun deletePlaylist(id: String) {
        viewModelScope.launch {
            if (_selectedPlaylistId.value == id) {
                _selectedPlaylistId.value = null
            }
            playlistRepository.deletePlaylist(id)
        }
    }

    fun renamePlaylist(id: String, newName: String) {
        viewModelScope.launch {
            playlistRepository.renamePlaylist(id, newName)
        }
    }

    fun addTrackToPlaylist(playlistId: String, trackId: Long, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val added = playlistRepository.addTrackToPlaylist(playlistId, trackId)
            onResult?.invoke(added)
        }
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: Long) {
        viewModelScope.launch {
            playlistRepository.removeTrackFromPlaylist(playlistId, trackId)
        }
    }
}
