package com.noirmusic.ui.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.noirmusic.data.model.Track
import com.noirmusic.data.playlist.Playlist
import com.noirmusic.playback.PlayerLogic
import com.noirmusic.ui.common.CenteredMessage
import com.noirmusic.ui.common.dragHandle
import com.noirmusic.ui.common.formatDuration
import com.noirmusic.ui.common.rememberDragReorderState
import com.noirmusic.ui.sakuga.SakugaButton
import com.noirmusic.ui.sakuga.SakugaButtonStyle
import com.noirmusic.ui.sakuga.SakugaDialog
import com.noirmusic.ui.sakuga.SakugaIconButton
import com.noirmusic.ui.sakuga.SakugaIconVfx
import com.noirmusic.ui.sakuga.SakugaPanel
import com.noirmusic.ui.sakuga.SakugaSnackbarHost
import com.noirmusic.ui.sakuga.sakugaCelShadow
import com.noirmusic.ui.sakuga.sakugaTrackTint
import com.noirmusic.ui.theme.SakugaTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailView(
    playlist: Playlist,
    tracks: List<Track>,
    favoriteIds: Set<Long>,
    currentMediaId: String?,
    isPlaying: Boolean = true,
    onBack: () -> Unit,
    onPlayTrack: (List<Track>, Int) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onRemoveTrack: (Long) -> Unit,
    onReorderTracks: (List<Long>) -> Unit,
    onRenamePlaylist: (String) -> Unit,
    onDeletePlaylist: () -> Unit,
    bottomBar: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val tokens = SakugaTheme.tokens
    var menuOpen by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(playlist.name) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val (heroBg, heroFg) = sakugaTrackTint(playlist.id.hashCode().toLong())

    val displayTracks = remember { mutableStateListOf<Track>() }
    val listState = rememberLazyListState()
    val dragState = rememberDragReorderState(
        listState = listState,
        onMove = { from, to ->
            if (from in displayTracks.indices && to in displayTracks.indices) {
                displayTracks.add(to, displayTracks.removeAt(from))
            }
        },
        onCommitMove = { _, _ ->
            onReorderTracks(displayTracks.map { it.id })
        },
    )

    LaunchedEffect(tracks, dragState.draggedKey) {
        if (dragState.draggedKey == null) {
            displayTracks.clear()
            displayTracks.addAll(tracks)
        }
    }

    val totalDurationMs = tracks.sumOf { it.durationMs }
    val durationText = if (totalDurationMs > 0) formatDuration(totalDurationMs) else "00:00"

    if (showRenameDialog) {
        SakugaDialog(onDismissRequest = { showRenameDialog = false }, horizontalPadding = 20.dp) {
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
                        value = renameText,
                        onValueChange = { renameText = it },
                        label = { Text("Nama Baru") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        SakugaButton(
                            onClick = { showRenameDialog = false },
                            style = SakugaButtonStyle.SECONDARY,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Batal", fontWeight = FontWeight.Bold)
                        }
                        SakugaButton(
                            onClick = {
                                val trimmed = renameText.trim()
                                if (trimmed.isNotBlank()) {
                                    onRenamePlaylist(trimmed)
                                    showRenameDialog = false
                                }
                            },
                            enabled = renameText.isNotBlank(),
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

    if (showDeleteConfirm) {
        SakugaDialog(onDismissRequest = { showDeleteConfirm = false }, horizontalPadding = 20.dp) {
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
                        text = "Playlist \"${playlist.name}\" akan dihapus. File lagu asli tidak akan terhapus.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        SakugaButton(
                            onClick = { showDeleteConfirm = false },
                            style = SakugaButtonStyle.SECONDARY,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Batal", fontWeight = FontWeight.Bold)
                        }
                        SakugaButton(
                            onClick = {
                                showDeleteConfirm = false
                                onDeletePlaylist()
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    SakugaIconButton(
                        onClick = onBack,
                        vfx = SakugaIconVfx.SKIP_PREV,
                        framed = true,
                        contentDescription = "Kembali",
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
                actions = {
                    Box {
                        SakugaIconButton(
                            onClick = { menuOpen = true },
                            vfx = SakugaIconVfx.SNAP,
                            framed = true,
                            contentDescription = "Menu playlist",
                        ) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            DropdownMenuItem(
                                text = { Text("Ganti Nama", fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    renameText = playlist.name
                                    showRenameDialog = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Hapus Playlist", fontWeight = FontWeight.Bold) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                },
                                onClick = {
                                    menuOpen = false
                                    showDeleteConfirm = true
                                },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        bottomBar = bottomBar,
        snackbarHost = { SakugaSnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Manga Hero Panel for Playlist Detail
            SakugaPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                shadowColor = tokens.celShadow,
                cornerRadius = 16.dp,
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Comic halftone corner accent inside hero panel
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val step = 12.dp.toPx()
                        var y = step
                        while (y < size.height) {
                            var x = size.width * 0.58f
                            while (x < size.width) {
                                val alpha = ((x - size.width * 0.58f) / (size.width * 0.42f)).coerceIn(0f, 1f)
                                drawCircle(
                                    color = tokens.halftoneDot,
                                    radius = 2.2.dp.toPx() * alpha,
                                    center = Offset(x, y),
                                )
                                x += step
                            }
                            y += step
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(84.dp)
                                    .graphicsLayer { rotationZ = -3.5f }
                                    .sakugaCelShadow(tokens.accent, cornerRadius = 14.dp, offsetX = 4.dp, offsetY = 5.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(heroBg)
                                    .border(2.5.dp, tokens.inkBorder, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                                    contentDescription = null,
                                    tint = heroFg,
                                    modifier = Modifier.size(44.dp),
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(tokens.cyanFill)
                                        .border(1.5.dp, tokens.inkBorder, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 7.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = "PLAYLIST LOKAL",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = tokens.onCyanFill,
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "${tracks.size} lagu · $durationText",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        if (tracks.isNotEmpty()) {
                            Spacer(Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                SakugaButton(
                                    onClick = { onPlayTrack(displayTracks.toList(), 0) },
                                    style = SakugaButtonStyle.PRIMARY,
                                    sfxText = "ドン!",
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Putar Semua", fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
                                }
                                SakugaButton(
                                    onClick = { onPlayTrack(displayTracks.shuffled(), 0) },
                                    style = SakugaButtonStyle.ACCENT,
                                    sfxText = "シュッ!",
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Acak", fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
                                }
                            }
                        }
                    }
                }
            }

            if (displayTracks.isEmpty()) {
                CenteredMessage(
                    icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                    title = "Playlist ini masih kosong",
                    body = "Buka tab Lagu, lalu ketuk menu opsi pada lagu yang ingin dimasukkan.",
                    actionLabel = "Lihat Lagu",
                    onAction = onBack,
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 18.dp),
                ) {
                    itemsIndexed(
                        items = displayTracks,
                        key = { _, t -> "${playlist.id}_${t.id}" },
                    ) { index, track ->
                        val itemKey = "${playlist.id}_${track.id}"
                        val isDragging = dragState.draggedKey == itemKey
                        val dismissState = rememberSwipeToDismissBoxState()

                        LaunchedEffect(dismissState.currentValue) {
                            if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                                onRemoveTrack(track.id)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Dihapus dari playlist: ${track.title}")
                                }
                            }
                        }

                        val itemModifier = Modifier
                            .then(if (!isDragging) Modifier.animateItem() else Modifier)
                            .zIndex(if (isDragging) 1f else 0f)
                            .graphicsLayer {
                                translationY = if (isDragging) dragState.draggingOffsetY else 0f
                            }

                        SwipeToDismissBox(
                            state = dismissState,
                            modifier = itemModifier,
                            enableDismissFromStartToEnd = false,
                            enableDismissFromEndToStart = dragState.draggedKey == null,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.errorContainer)
                                        .border(2.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Hapus dari playlist",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                }
                            },
                        ) {
                            TrackRow(
                                track = track,
                                isCurrent = currentMediaId == track.id.toString(),
                                isPlaying = isPlaying,
                                trackIndex = index + 1,
                                isFavorite = favoriteIds.contains(track.id),
                                onClick = { onPlayTrack(displayTracks.toList(), index) },
                                onPlayNext = {
                                    onPlayNext(track)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Putar berikutnya: ${track.title}")
                                    }
                                },
                                onAddToQueue = {
                                    onAddToQueue(track)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Ditambahkan ke antrean: ${track.title}")
                                    }
                                },
                                onToggleFavorite = { onToggleFavorite(track.id) },
                                onMoveUp = if (index > 0) {
                                    {
                                        val reordered = PlayerLogic.moveListItem(displayTracks, index, index - 1)
                                        displayTracks.clear()
                                        displayTracks.addAll(reordered)
                                        onReorderTracks(reordered.map { it.id })
                                    }
                                } else {
                                    null
                                },
                                onMoveDown = if (index < displayTracks.lastIndex) {
                                    {
                                        val reordered = PlayerLogic.moveListItem(displayTracks, index, index + 1)
                                        displayTracks.clear()
                                        displayTracks.addAll(reordered)
                                        onReorderTracks(reordered.map { it.id })
                                    }
                                } else {
                                    null
                                },
                                onRemoveFromPlaylist = {
                                    onRemoveTrack(track.id)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Dihapus dari playlist: ${track.title}")
                                    }
                                },
                                dragHandleModifier = if (displayTracks.size > 1) {
                                    Modifier.dragHandle(dragState, itemKey)
                                } else {
                                    null
                                },
                                isDragging = isDragging,
                            )
                        }
                    }
                }
            }
        }
    }
}
