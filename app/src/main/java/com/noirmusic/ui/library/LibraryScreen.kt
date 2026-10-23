package com.noirmusic.ui.library

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.noirmusic.data.media.SortOrder
import com.noirmusic.data.model.Track
import com.noirmusic.data.playlist.Playlist
import com.noirmusic.ui.common.CenteredMessage
import com.noirmusic.ui.common.LoadingView
import com.noirmusic.ui.player.MiniPlayer
import com.noirmusic.ui.player.PlaybackUiState

enum class LibraryTab { SONGS, PLAYLISTS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    query: String,
    sort: SortOrder,
    playback: PlaybackUiState,
    playlists: List<Playlist>,
    selectedPlaylistWithTracks: Pair<Playlist, List<Track>>?,
    onQueryChange: (String) -> Unit,
    onSortChange: (SortOrder) -> Unit,
    onRefresh: () -> Unit,
    onTrackClick: (List<Track>, Int) -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onSelectPlaylist: (String?) -> Unit,
    onCreatePlaylist: (name: String, initialTrackId: Long?) -> Unit,
    onDeletePlaylist: (id: String) -> Unit,
    onRenamePlaylist: (id: String, newName: String) -> Unit,
    onAddToPlaylist: (playlistId: String, trackId: Long) -> Unit,
    onRemoveFromPlaylist: (playlistId: String, trackId: Long) -> Unit,
) {
    if (selectedPlaylistWithTracks != null) {
        val (playlist, tracks) = selectedPlaylistWithTracks
        PlaylistDetailView(
            playlist = playlist,
            tracks = tracks,
            currentMediaId = playback.nowPlaying?.mediaId,
            onBack = { onSelectPlaylist(null) },
            onPlayTrack = onTrackClick,
            onRemoveTrack = { trackId -> onRemoveFromPlaylist(playlist.id, trackId) },
            onRenamePlaylist = { newName -> onRenamePlaylist(playlist.id, newName) },
            onDeletePlaylist = { onDeletePlaylist(playlist.id) },
        )
        return
    }

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var trackToAddToPlaylist by remember { mutableStateOf<Track?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistNameText by remember { mutableStateOf("") }

    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("Playlist Baru") },
            text = {
                OutlinedTextField(
                    value = newPlaylistNameText,
                    onValueChange = { newPlaylistNameText = it },
                    label = { Text("Nama Playlist") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newPlaylistNameText.trim()
                        if (trimmed.isNotBlank()) {
                            onCreatePlaylist(trimmed, null)
                            newPlaylistNameText = ""
                            showCreatePlaylistDialog = false
                        }
                    },
                    enabled = newPlaylistNameText.isNotBlank(),
                ) {
                    Text("Buat")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Batal")
                }
            },
        )
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
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .border(
                                        width = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(8.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.GraphicEq,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Text(
                                text = "Noir Music",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                    actions = {
                        if (selectedTabIndex == 0) {
                            Box {
                                IconButton(onClick = { sortMenuOpen = true }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Sort,
                                        contentDescription = "Urutkan lagu",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                DropdownMenu(
                                    expanded = sortMenuOpen,
                                    onDismissRequest = { sortMenuOpen = false },
                                ) {
                                    SortOrder.entries.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option.label) },
                                            trailingIcon = if (option == sort) {
                                                {
                                                    Icon(
                                                        Icons.Filled.Check,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
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
                        } else {
                            IconButton(onClick = {
                                newPlaylistNameText = ""
                                showCreatePlaylistDialog = true
                            }) {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = "Buat playlist baru",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        IconButton(onClick = onRefresh) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = "Muat ulang",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                )

                // Storage Telemetry Banner (Stitch Screen 1)
                val totalTracksCount = (state as? LibraryUiState.Content)?.all?.size ?: 0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp),
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
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = if (totalTracksCount > 0) "$totalTracksCount local tracks scanned" else "Local audio storage",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = "STORAGE: LOCAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }

                PrimaryTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = {
                            val count = (state as? LibraryUiState.Content)?.all?.size
                            Text(if (count != null) "Lagu ($count)" else "Lagu")
                        },
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("Playlist (${playlists.size})") },
                    )
                }
            }
        },
        bottomBar = {
            if (playback.nowPlaying != null) {
                MiniPlayer(
                    state = playback,
                    onClick = onOpenNowPlaying,
                    onTogglePlayPause = onTogglePlayPause,
                    onNext = onNext,
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (selectedTabIndex) {
                0 -> {
                    when (state) {
                        LibraryUiState.Loading -> LoadingView("Memindai file musik…")
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
                            currentMediaId = playback.nowPlaying?.mediaId,
                            onQueryChange = onQueryChange,
                            onTrackClick = onTrackClick,
                            onAddToPlaylist = { track -> trackToAddToPlaylist = track },
                        )
                    }
                }
                1 -> {
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
    var playlistToRename by remember { mutableStateOf<Playlist?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }

    if (playlistToRename != null) {
        AlertDialog(
            onDismissRequest = { playlistToRename = null },
            title = { Text("Ganti Nama Playlist") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Nama Baru") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = renameInput.trim()
                        if (trimmed.isNotBlank()) {
                            onRenamePlaylist(playlistToRename!!.id, trimmed)
                            playlistToRename = null
                        }
                    },
                    enabled = renameInput.isNotBlank(),
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToRename = null }) {
                    Text("Batal")
                }
            },
        )
    }

    if (playlistToDelete != null) {
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            title = { Text("Hapus Playlist?") },
            text = { Text("Playlist “${playlistToDelete!!.name}” akan dihapus.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePlaylist(playlistToDelete!!.id)
                        playlistToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToDelete = null }) {
                    Text("Batal")
                }
            },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Quick Action: Create Playlist Card (Stitch Screen 4)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable(onClick = onCreatePlaylist),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .border(
                                width = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(8.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column {
                        Text(
                            text = "Buat Playlist Baru",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Atur dan simpan daftar lagu favorit",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        if (playlists.isEmpty()) {
            CenteredMessage(
                icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                title = "Belum ada playlist",
                body = "Ketuk “Buat Playlist Baru” untuk mulai menyusun daftar lagu favorit Anda.",
                actionLabel = "Buat Playlist",
                onAction = onCreatePlaylist,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    var menuExpanded by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectPlaylist(playlist.id) },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
                                    .border(
                                        width = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(8.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${playlist.trackIds.size} lagu • LOCAL ARCHIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            Box {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(
                                        imageVector = Icons.Filled.MoreVert,
                                        contentDescription = "Opsi playlist",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Ganti Nama") },
                                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            renameInput = playlist.name
                                            playlistToRename = playlist
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Hapus") },
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
    currentMediaId: String?,
    onQueryChange: (String) -> Unit,
    onTrackClick: (List<Track>, Int) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val total = state.all.size
    val visible = state.visible.size
    val countLabel = if (query.isBlank()) {
        "$total ${if (total == 1) "lagu" else "lagu"}"
    } else {
        "$visible dari $total lagu"
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            placeholder = {
                Text(
                    "Cari lagu, artis, atau album",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Hapus pencarian")
                    }
                }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(8.dp),
                ),
        )

        // Category Filter & Quick Shuffle Bar (Stitch Screen 1)
        if (state.visible.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = countLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.clickable {
                        val shuffled = state.visible.shuffled()
                        onTrackClick(shuffled, 0)
                    },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shuffle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "Acak Semua",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
        if (state.visible.isEmpty()) {
            CenteredMessage(
                icon = Icons.Filled.SearchOff,
                title = "Tidak ada lagu cocok dengan “${state.query}”",
                body = "Coba gunakan kata kunci lain atau hapus filter.",
                actionLabel = "Hapus filter",
                onAction = { onQueryChange("") },
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                itemsIndexed(
                    items = state.visible,
                    key = { _, t -> t.id },
                    contentType = { _, _ -> "track" },
                ) { index, track ->
                    TrackRow(
                        track = track,
                        isCurrent = currentMediaId == track.id.toString(),
                        onClick = { onTrackClick(state.visible, index) },
                        onAddToPlaylist = { onAddToPlaylist(track) },
                    )
                }
            }
        }
    }
}
