package com.noirmusic.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.noirmusic.data.media.SortOrder
import com.noirmusic.data.media.TrackListLogic
import com.noirmusic.data.model.Track
import com.noirmusic.data.playlist.Playlist
import com.noirmusic.ui.common.CenteredMessage
import com.noirmusic.ui.common.LoadingView
import com.noirmusic.ui.player.MiniPlayer
import com.noirmusic.ui.player.PlaybackUiState
import com.noirmusic.ui.sakuga.LocalSakugaFx
import com.noirmusic.ui.sakuga.SakugaButton
import com.noirmusic.ui.sakuga.SakugaButtonStyle
import com.noirmusic.ui.sakuga.SakugaDialog
import com.noirmusic.ui.sakuga.SakugaEqualizer
import com.noirmusic.ui.sakuga.SakugaIconButton
import com.noirmusic.ui.sakuga.SakugaIconVfx
import com.noirmusic.ui.sakuga.SakugaPanel
import com.noirmusic.ui.sakuga.SakugaSearchField
import com.noirmusic.ui.sakuga.SakugaSegmentedSwitch
import com.noirmusic.ui.sakuga.SakugaSnackbarHost
import com.noirmusic.ui.sakuga.SakugaTabRow
import com.noirmusic.ui.sakuga.sakugaCelShadow
import com.noirmusic.ui.sakuga.sakugaTrackTint
import com.noirmusic.ui.sakuga.trackRootCenter
import com.noirmusic.ui.theme.SakugaTheme
import com.noirmusic.ui.theme.SakugaThemeMode
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class LibraryTab { SONGS, FAVORITES, HISTORY, PLAYLISTS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    query: String,
    sort: SortOrder,
    playback: PlaybackUiState,
    positionMsFlow: StateFlow<Long>,
    playlists: List<Playlist>,
    favoriteIds: Set<Long>,
    favoriteTracks: List<Track>,
    recentTracks: List<TrackPlayEntry>,
    mostPlayedTracks: List<TrackPlayEntry>,
    selectedPlaylistWithTracks: Pair<Playlist, List<Track>>?,
    themeMode: SakugaThemeMode,
    onToggleTheme: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSortChange: (SortOrder) -> Unit,
    onRefresh: () -> Unit,
    onTrackClick: (List<Track>, Int) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onSelectPlaylist: (String?) -> Unit,
    onCreatePlaylist: (name: String, initialTrackId: Long?) -> Unit,
    onDeletePlaylist: (id: String) -> Unit,
    onRenamePlaylist: (id: String, newName: String) -> Unit,
    onAddToPlaylist: (playlistId: String, trackId: Long) -> Unit,
    onRemoveFromPlaylist: (playlistId: String, trackId: Long) -> Unit,
    onReorderPlaylistTracks: (playlistId: String, newTrackIds: List<Long>) -> Unit,
) {
    val tokens = SakugaTheme.tokens

    val miniPlayerBar: @Composable () -> Unit = {
        if (playback.nowPlaying != null) {
            MiniPlayer(
                state = playback,
                positionMsFlow = positionMsFlow,
                onClick = onOpenNowPlaying,
                onTogglePlayPause = onTogglePlayPause,
                onNext = onNext,
            )
        }
    }

    if (selectedPlaylistWithTracks != null) {
        val (playlist, tracks) = selectedPlaylistWithTracks
        PlaylistDetailView(
            playlist = playlist,
            tracks = tracks,
            favoriteIds = favoriteIds,
            currentMediaId = playback.nowPlaying?.mediaId,
            isPlaying = playback.isPlaying,
            onBack = { onSelectPlaylist(null) },
            onPlayTrack = onTrackClick,
            onPlayNext = onPlayNext,
            onAddToQueue = onAddToQueue,
            onToggleFavorite = onToggleFavorite,
            onRemoveTrack = { trackId -> onRemoveFromPlaylist(playlist.id, trackId) },
            onReorderTracks = { newTrackIds -> onReorderPlaylistTracks(playlist.id, newTrackIds) },
            onRenamePlaylist = { newName -> onRenamePlaylist(playlist.id, newName) },
            onDeletePlaylist = { onDeletePlaylist(playlist.id) },
            bottomBar = miniPlayerBar,
        )
        return
    }

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var trackToAddToPlaylist by remember { mutableStateOf<Track?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistNameText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()

    if (showCreatePlaylistDialog) {
        SakugaDialog(onDismissRequest = { showCreatePlaylistDialog = false }, horizontalPadding = 20.dp) {
            SakugaPanel(
                modifier = Modifier.fillMaxWidth(),
                shadowColor = tokens.accent,
                cornerRadius = 16.dp,
                contentPadding = PaddingValues(20.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Playlist Baru", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPlaylistNameText,
                        onValueChange = { newPlaylistNameText = it },
                        label = { Text("Nama Playlist") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        SakugaButton(
                            onClick = { showCreatePlaylistDialog = false },
                            style = SakugaButtonStyle.SECONDARY,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Batal", fontWeight = FontWeight.Bold)
                        }
                        SakugaButton(
                            onClick = {
                                val trimmed = newPlaylistNameText.trim()
                                if (trimmed.isNotBlank()) {
                                    onCreatePlaylist(trimmed, null)
                                    newPlaylistNameText = ""
                                    showCreatePlaylistDialog = false
                                }
                            },
                            enabled = newPlaylistNameText.isNotBlank(),
                            style = SakugaButtonStyle.ACCENT,
                            sfxText = "ドン!",
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Buat", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }

    trackToAddToPlaylist?.let { track ->
        AddToPlaylistDialog(
            track = track,
            playlists = playlists,
            onAddToPlaylist = { playlistId ->
                onAddToPlaylist(playlistId, track.id)
                trackToAddToPlaylist = null
            },
            onCreateAndAdd = { name ->
                onCreatePlaylist(name, track.id)
                trackToAddToPlaylist = null
            },
            onDismiss = { trackToAddToPlaylist = null },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .graphicsLayer { rotationZ = -4f }
                                    .sakugaCelShadow(tokens.accent, cornerRadius = 10.dp, offsetX = 2.5.dp, offsetY = 3.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                    .border(2.2.dp, tokens.inkBorder, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                SakugaEqualizer(isPlaying = playback.isPlaying)
                            }
                            Column {
                                Text(
                                    text = "Noir Music",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                    actions = {
                        // Daylight / Midnight Sakuga theme switch
                        SakugaIconButton(
                            onClick = onToggleTheme,
                            vfx = SakugaIconVfx.THEME_SHIFT,
                            framed = true,
                            frameColor = if (themeMode == SakugaThemeMode.DAYLIGHT) tokens.violetFill else tokens.cyanFill,
                            contentDescription = if (themeMode == SakugaThemeMode.DAYLIGHT) {
                                "Aktifkan tema Sakuga Midnight"
                            } else {
                                "Aktifkan tema Sakuga Daylight"
                            },
                        ) {
                            Icon(
                                imageVector = if (themeMode == SakugaThemeMode.DAYLIGHT) {
                                    Icons.Filled.DarkMode
                                } else {
                                    Icons.Filled.LightMode
                                },
                                contentDescription = null,
                                tint = if (themeMode == SakugaThemeMode.DAYLIGHT) {
                                    tokens.onVioletFill
                                } else {
                                    tokens.onCyanFill
                                },
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        if (selectedTabIndex == LibraryTab.SONGS.ordinal) {
                            Box {
                                SakugaIconButton(
                                    onClick = { sortMenuOpen = true },
                                    vfx = SakugaIconVfx.SNAP,
                                    framed = true,
                                    contentDescription = "Urutkan lagu",
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Sort,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                DropdownMenu(
                                    expanded = sortMenuOpen,
                                    onDismissRequest = { sortMenuOpen = false },
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                ) {
                                    SortOrder.entries.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option.label, fontWeight = FontWeight.Bold) },
                                            trailingIcon = if (option == sort) {
                                                {
                                                    Icon(
                                                        Icons.Filled.Check,
                                                        contentDescription = null,
                                                        tint = tokens.accentText,
                                                    )
                                                }
                                            } else {
                                                null
                                            },
                                            onClick = {
                                                sortMenuOpen = false
                                                onSortChange(option)
                                            },
                                        )
                                    }
                                }
                            }
                        } else if (selectedTabIndex == LibraryTab.PLAYLISTS.ordinal) {
                            SakugaIconButton(
                                onClick = {
                                    newPlaylistNameText = ""
                                    showCreatePlaylistDialog = true
                                },
                                vfx = SakugaIconVfx.PRIMARY_CONFIRM,
                                framed = true,
                                frameColor = tokens.accent,
                                contentDescription = "Buat playlist baru",
                            ) {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = null,
                                    tint = tokens.onAccent,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }

                        SakugaIconButton(
                            onClick = onRefresh,
                            vfx = SakugaIconVfx.SKIP_NEXT,
                            framed = true,
                            contentDescription = "Muat ulang",
                        ) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    },
                )

                // Kinetic Archive Status Ribbon
                val totalTracksCount = (state as? LibraryUiState.Content)?.all?.size ?: 0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .border(1.5.dp, tokens.inkBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Storage,
                            contentDescription = null,
                            tint = tokens.accentText,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = if (totalTracksCount > 0) "$totalTracksCount lagu lokal siap diputar" else "Penyimpanan audio lokal",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (themeMode == SakugaThemeMode.DAYLIGHT) tokens.cyanFill else tokens.violetFill)
                            .border(1.dp, tokens.inkBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = if (themeMode == SakugaThemeMode.DAYLIGHT) "SAKUGA DAY" else "SAKUGA NIGHT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            color = if (themeMode == SakugaThemeMode.DAYLIGHT) tokens.onCyanFill else tokens.onVioletFill,
                        )
                    }
                }

                if (!playback.isConnected && playback.errorMessage != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .border(2.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = playback.errorMessage,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                SakugaTabRow(
                    tabs = listOf("Lagu", "Favorit", "Riwayat", "Playlist"),
                    selectedIndex = selectedTabIndex,
                    onSelect = { selectedTabIndex = it },
                )
            }
        },
        bottomBar = miniPlayerBar,
        snackbarHost = { SakugaSnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (selectedTabIndex) {
                LibraryTab.SONGS.ordinal -> {
                    when (state) {
                        LibraryUiState.Loading -> LoadingView("Memindai file musik...")
                        LibraryUiState.Empty -> CenteredMessage(
                            icon = Icons.Filled.LibraryMusic,
                            title = "Tidak ada musik ditemukan",
                            body = "Salin file audio ke memori ponsel (folder Music atau Download), lalu ketuk Muat Ulang.",
                            actionLabel = "Muat Ulang",
                            onAction = onRefresh,
                        )
                        is LibraryUiState.Error -> CenteredMessage(
                            icon = Icons.Filled.ErrorOutline,
                            title = "Gagal membaca library musik",
                            body = state.message,
                            actionLabel = "Coba lagi",
                            onAction = onRefresh,
                        )
                        is LibraryUiState.Content -> LibraryContent(
                            state = state,
                            query = query,
                            favoriteIds = favoriteIds,
                            currentMediaId = playback.nowPlaying?.mediaId,
                            isPlaying = playback.isPlaying,
                            onQueryChange = onQueryChange,
                            onTrackClick = onTrackClick,
                            onPlayNext = { track ->
                                onPlayNext(track)
                                snackbarScope.launch {
                                    snackbarHostState.showSnackbar("Putar berikutnya: ${track.title}")
                                }
                            },
                            onAddToQueue = { track ->
                                onAddToQueue(track)
                                snackbarScope.launch {
                                    snackbarHostState.showSnackbar("Ditambahkan ke antrean: ${track.title}")
                                }
                            },
                            onToggleFavorite = onToggleFavorite,
                            onAddToPlaylist = { track -> trackToAddToPlaylist = track },
                        )
                    }
                }
                LibraryTab.FAVORITES.ordinal -> {
                    FavoritesTabContent(
                        tracks = favoriteTracks,
                        currentMediaId = playback.nowPlaying?.mediaId,
                        isPlaying = playback.isPlaying,
                        onTrackClick = onTrackClick,
                        onPlayNext = { track ->
                            onPlayNext(track)
                            snackbarScope.launch {
                                snackbarHostState.showSnackbar("Putar berikutnya: ${track.title}")
                            }
                        },
                        onAddToQueue = { track ->
                            onAddToQueue(track)
                            snackbarScope.launch {
                                snackbarHostState.showSnackbar("Ditambahkan ke antrean: ${track.title}")
                            }
                        },
                        onToggleFavorite = onToggleFavorite,
                        onAddToPlaylist = { track -> trackToAddToPlaylist = track },
                        onExploreSongs = { selectedTabIndex = LibraryTab.SONGS.ordinal },
                    )
                }
                LibraryTab.HISTORY.ordinal -> {
                    HistoryTabContent(
                        recentTracks = recentTracks,
                        mostPlayedTracks = mostPlayedTracks,
                        favoriteIds = favoriteIds,
                        currentMediaId = playback.nowPlaying?.mediaId,
                        isPlaying = playback.isPlaying,
                        onTrackClick = onTrackClick,
                        onPlayNext = { track ->
                            onPlayNext(track)
                            snackbarScope.launch {
                                snackbarHostState.showSnackbar("Putar berikutnya: ${track.title}")
                            }
                        },
                        onAddToQueue = { track ->
                            onAddToQueue(track)
                            snackbarScope.launch {
                                snackbarHostState.showSnackbar("Ditambahkan ke antrean: ${track.title}")
                            }
                        },
                        onToggleFavorite = onToggleFavorite,
                        onAddToPlaylist = { track -> trackToAddToPlaylist = track },
                        onExploreSongs = { selectedTabIndex = LibraryTab.SONGS.ordinal },
                    )
                }
                LibraryTab.PLAYLISTS.ordinal -> {
                    PlaylistsTabContent(
                        playlists = playlists,
                        onSelectPlaylist = onSelectPlaylist,
                        onCreatePlaylist = {
                            newPlaylistNameText = ""
                            showCreatePlaylistDialog = true
                        },
                        onDeletePlaylist = onDeletePlaylist,
                        onRenamePlaylist = onRenamePlaylist,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistsTabContent(
    playlists: List<Playlist>,
    onSelectPlaylist: (String) -> Unit,
    onCreatePlaylist: () -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onRenamePlaylist: (String, String) -> Unit,
) {
    val tokens = SakugaTheme.tokens
    val fx = LocalSakugaFx.current
    var playlistToRename by remember { mutableStateOf<Playlist?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }

    if (playlistToRename != null) {
        SakugaDialog(onDismissRequest = { playlistToRename = null }, horizontalPadding = 20.dp) {
            SakugaPanel(
                modifier = Modifier.fillMaxWidth(),
                shadowColor = tokens.cyanFill,
                cornerRadius = 16.dp,
                contentPadding = PaddingValues(20.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Ganti Nama Playlist", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        label = { Text("Nama Baru") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        SakugaButton(
                            onClick = { playlistToRename = null },
                            style = SakugaButtonStyle.SECONDARY,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Batal", fontWeight = FontWeight.Bold)
                        }
                        SakugaButton(
                            onClick = {
                                val trimmed = renameInput.trim()
                                if (trimmed.isNotBlank()) {
                                    onRenamePlaylist(playlistToRename!!.id, trimmed)
                                    playlistToRename = null
                                }
                            },
                            enabled = renameInput.isNotBlank(),
                            style = SakugaButtonStyle.ACCENT,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Simpan", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }

    if (playlistToDelete != null) {
        SakugaDialog(onDismissRequest = { playlistToDelete = null }, horizontalPadding = 20.dp) {
            SakugaPanel(
                modifier = Modifier.fillMaxWidth(),
                shadowColor = MaterialTheme.colorScheme.error,
                cornerRadius = 16.dp,
                contentPadding = PaddingValues(20.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Hapus Playlist?", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Playlist \"${playlistToDelete!!.name}\" akan dihapus.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        SakugaButton(
                            onClick = { playlistToDelete = null },
                            style = SakugaButtonStyle.SECONDARY,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Batal", fontWeight = FontWeight.Bold)
                        }
                        SakugaButton(
                            onClick = {
                                onDeletePlaylist(playlistToDelete!!.id)
                                playlistToDelete = null
                            },
                            style = SakugaButtonStyle.DANGER,
                            sfxText = "ズザッ",
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Hapus", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Bold Create Playlist hero banner
        var createCenter by remember { mutableStateOf(Offset.Zero) }
        SakugaPanel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .trackRootCenter { createCenter = it }
                .clickable {
                    if (createCenter != Offset.Zero) {
                        fx.emitPrimaryConfirm(createCenter, halfWDp = 100f, halfHDp = 24f, sfx = "ドン!")
                    }
                    onCreatePlaylist()
                },
            containerColor = tokens.cyanFill,
            borderColor = tokens.inkBorder,
            shadowColor = tokens.accent,
            cornerRadius = 14.dp,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .border(2.dp, tokens.inkBorder, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            tint = tokens.accentText,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Column {
                        Text(
                            text = "Buat Playlist Baru",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            color = tokens.onCyanFill,
                        )
                        Text(
                            text = "Susun daftar lagu favorit Anda",
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.onCyanFill.copy(alpha = 0.82f),
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = tokens.onCyanFill,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        if (playlists.isEmpty()) {
            CenteredMessage(
                icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                title = "Belum ada playlist",
                body = "Ketuk Buat Playlist Baru untuk mulai menyusun daftar lagu favorit Anda.",
                actionLabel = "Buat Playlist",
                onAction = onCreatePlaylist,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    var menuExpanded by remember { mutableStateOf(false) }
                    var cardCenter by remember { mutableStateOf(Offset.Zero) }
                    val (tintBg, tintFg) = sakugaTrackTint(playlist.id.hashCode().toLong())

                    SakugaPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .trackRootCenter { cardCenter = it }
                            .clickable {
                                if (cardCenter != Offset.Zero) {
                                    fx.emitSecondarySnap(cardCenter, sfx = "カチッ")
                                }
                                onSelectPlaylist(playlist.id)
                            },
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        shadowColor = tokens.celShadow,
                        cornerRadius = 14.dp,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(tintBg)
                                    .border(2.dp, tokens.inkBorder, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                                    contentDescription = null,
                                    tint = tintFg,
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${playlist.trackIds.size} lagu",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            Box {
                                SakugaIconButton(
                                    onClick = { menuExpanded = true },
                                    vfx = SakugaIconVfx.SNAP,
                                    contentDescription = "Opsi playlist",
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MoreVert,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false },
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Ganti Nama", fontWeight = FontWeight.Bold) },
                                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            renameInput = playlist.name
                                            playlistToRename = playlist
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Hapus", fontWeight = FontWeight.Bold) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Filled.Delete,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                            )
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            playlistToDelete = playlist
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryContent(
    state: LibraryUiState.Content,
    query: String,
    favoriteIds: Set<Long>,
    currentMediaId: String?,
    isPlaying: Boolean,
    onQueryChange: (String) -> Unit,
    onTrackClick: (List<Track>, Int) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val total = state.all.size
    val visible = state.visible.size
    val countLabel = if (query.isBlank()) {
        "$total lagu"
    } else {
        "$visible dari $total lagu"
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SakugaSearchField(
            query = query,
            onQueryChange = onQueryChange,
            onSearchAction = { focusManager.clearFocus() },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )

        if (state.visible.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = countLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                SakugaButton(
                    onClick = {
                        val shuffled = state.visible.shuffled()
                        onTrackClick(shuffled, 0)
                    },
                    style = SakugaButtonStyle.ACCENT,
                    cornerRadius = 10.dp,
                    sfxText = "ドン!",
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Acak Semua",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                    )
                }
            }
        }

        if (state.visible.isEmpty()) {
            CenteredMessage(
                icon = Icons.Filled.SearchOff,
                title = "Tidak ada lagu cocok dengan \"${state.query}\"",
                body = "Coba gunakan kata kunci lain atau hapus filter.",
                actionLabel = "Hapus filter",
                onAction = { onQueryChange("") },
            )
        } else {
            val listState = rememberLazyListState()
            val coroutineScope = rememberCoroutineScope()
            val availableSymbols = remember(state.visible, state.sort) {
                TrackListLogic.availableSymbolsFor(state.visible, state.sort)
            }
            val currentSymbol by remember(state.visible, state.sort) {
                derivedStateOf {
                    state.visible.getOrNull(listState.firstVisibleItemIndex)?.let {
                        TrackListLogic.sectionCharFor(it, state.sort)
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(end = 34.dp, top = 2.dp, bottom = 18.dp),
                ) {
                    itemsIndexed(
                        items = state.visible,
                        key = { _, t -> t.id },
                        contentType = { _, _ -> "track" },
                    ) { index, track ->
                        TrackRow(
                            track = track,
                            isCurrent = currentMediaId == track.id.toString(),
                            isPlaying = isPlaying,
                            isFavorite = favoriteIds.contains(track.id),
                            onClick = {
                                val fullIndex = state.allSorted.indexOfFirst { it.id == track.id }
                                if (fullIndex >= 0) {
                                    onTrackClick(state.allSorted, fullIndex)
                                } else {
                                    onTrackClick(state.visible, index)
                                }
                            },
                            onPlayNext = { onPlayNext(track) },
                            onAddToQueue = { onAddToQueue(track) },
                            onToggleFavorite = { onToggleFavorite(track.id) },
                            onAddToPlaylist = { onAddToPlaylist(track) },
                        )
                    }
                }

                AlphabetFastScroller(
                    availableSymbols = availableSymbols,
                    currentSymbol = currentSymbol,
                    onSymbolSelected = { symbol ->
                        val targetIndex = TrackListLogic.findTargetIndexForSymbol(
                            tracks = state.visible,
                            symbol = symbol,
                            sort = state.sort,
                        )
                        if (targetIndex != null && targetIndex in state.visible.indices) {
                            coroutineScope.launch {
                                listState.scrollToItem(targetIndex)
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(vertical = 4.dp, horizontal = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun FavoritesTabContent(
    tracks: List<Track>,
    currentMediaId: String?,
    isPlaying: Boolean,
    onTrackClick: (List<Track>, Int) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onExploreSongs: () -> Unit,
) {
    if (tracks.isEmpty()) {
        CenteredMessage(
            icon = Icons.Filled.FavoriteBorder,
            title = "Belum ada lagu favorit",
            body = "Ketuk ikon hati di Now Playing atau pilih Tambah ke Favorit dari menu lagu.",
            actionLabel = "Lihat Lagu",
            onAction = onExploreSongs,
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${tracks.size} lagu favorit",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SakugaButton(
                    onClick = { onTrackClick(tracks, 0) },
                    style = SakugaButtonStyle.CYAN,
                    cornerRadius = 10.dp,
                    sfxText = "ドン!",
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Putar", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
                }
                SakugaButton(
                    onClick = { onTrackClick(tracks.shuffled(), 0) },
                    style = SakugaButtonStyle.ACCENT,
                    cornerRadius = 10.dp,
                    sfxText = "シュッ!",
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Acak", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 18.dp),
        ) {
            itemsIndexed(
                items = tracks,
                key = { _, t -> "fav_${t.id}" },
            ) { index, track ->
                TrackRow(
                    track = track,
                    isCurrent = currentMediaId == track.id.toString(),
                    isPlaying = isPlaying,
                    trackIndex = index + 1,
                    isFavorite = true,
                    onClick = { onTrackClick(tracks, index) },
                    onPlayNext = { onPlayNext(track) },
                    onAddToQueue = { onAddToQueue(track) },
                    onToggleFavorite = { onToggleFavorite(track.id) },
                    onAddToPlaylist = { onAddToPlaylist(track) },
                )
            }
        }
    }
}

private enum class HistoryMode { RECENT, MOST_PLAYED }

@Composable
private fun HistoryTabContent(
    recentTracks: List<TrackPlayEntry>,
    mostPlayedTracks: List<TrackPlayEntry>,
    favoriteIds: Set<Long>,
    currentMediaId: String?,
    isPlaying: Boolean,
    onTrackClick: (List<Track>, Int) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onExploreSongs: () -> Unit,
) {
    var mode by rememberSaveable { mutableStateOf(HistoryMode.RECENT) }
    val activeEntries = if (mode == HistoryMode.RECENT) recentTracks else mostPlayedTracks

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            SakugaSegmentedSwitch(
                options = listOf("Baru Diputar", "Sering Diputar"),
                selectedIndex = mode.ordinal,
                onSelect = { idx -> mode = if (idx == 0) HistoryMode.RECENT else HistoryMode.MOST_PLAYED },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (activeEntries.isEmpty()) {
            CenteredMessage(
                icon = Icons.Filled.History,
                title = "Belum ada riwayat pemutaran",
                body = "Mulai putar lagu dari library untuk melacak lagu terakhir dan lagu yang paling sering diputar.",
                actionLabel = "Lihat Lagu",
                onAction = onExploreSongs,
            )
        } else {
            val activeTracks = remember(activeEntries) { activeEntries.map { it.track } }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 18.dp),
            ) {
                itemsIndexed(
                    items = activeEntries,
                    key = { _, entry -> "${mode.name}_${entry.track.id}" },
                ) { index, entry ->
                    TrackRow(
                        track = entry.track,
                        isCurrent = currentMediaId == entry.track.id.toString(),
                        isPlaying = isPlaying,
                        trackIndex = index + 1,
                        isFavorite = favoriteIds.contains(entry.track.id),
                        subtitleSuffix = "${entry.playCount}x diputar",
                        onClick = { onTrackClick(activeTracks, index) },
                        onPlayNext = { onPlayNext(entry.track) },
                        onAddToQueue = { onAddToQueue(entry.track) },
                        onToggleFavorite = { onToggleFavorite(entry.track.id) },
                        onAddToPlaylist = { onAddToPlaylist(entry.track) },
                    )
                }
            }
        }
    }
}
