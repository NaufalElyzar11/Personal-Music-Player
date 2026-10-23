package com.noirmusic.ui.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.noirmusic.data.media.SortOrder
import com.noirmusic.data.model.Track
import com.noirmusic.ui.common.CenteredMessage
import com.noirmusic.ui.common.LoadingView
import com.noirmusic.ui.player.MiniPlayer
import com.noirmusic.ui.player.PlaybackUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    query: String,
    sort: SortOrder,
    playback: PlaybackUiState,
    onQueryChange: (String) -> Unit,
    onSortChange: (SortOrder) -> Unit,
    onRefresh: () -> Unit,
    onTrackClick: (List<Track>, Int) -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onOpenNowPlaying: () -> Unit,
) {
    var sortMenuOpen by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Noir Music", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                actions = {
                    Box {
                        IconButton(onClick = { sortMenuOpen = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort songs")
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
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh library")
                    }
                },
            )
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
            when (state) {
                LibraryUiState.Loading -> LoadingView("Scanning your music…")
                LibraryUiState.Empty -> CenteredMessage(
                    icon = Icons.Filled.LibraryMusic,
                    title = "No music found",
                    body = "Copy audio files to your phone (e.g. the Music or Download folder), then tap Refresh.",
                    actionLabel = "Refresh",
                    onAction = onRefresh,
                )
                is LibraryUiState.Error -> CenteredMessage(
                    icon = Icons.Filled.ErrorOutline,
                    title = "Couldn't read your music library",
                    body = state.message,
                    actionLabel = "Try again",
                    onAction = onRefresh,
                )
                is LibraryUiState.Content -> LibraryContent(
                    state = state,
                    query = query,
                    currentMediaId = playback.nowPlaying?.mediaId,
                    onQueryChange = onQueryChange,
                    onTrackClick = onTrackClick,
                )
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
) {
    val focusManager = LocalFocusManager.current
    val total = state.all.size
    val visible = state.visible.size
    val countLabel = if (query.isBlank()) {
        "$total ${if (total == 1) "song" else "songs"}"
    } else {
        "$visible of $total ${if (total == 1) "song" else "songs"}"
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            placeholder = { Text("Search songs, artists, albums") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear search")
                    }
                }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            shape = RoundedCornerShape(28.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
        )
        Text(
            text = countLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
        )
        if (state.visible.isEmpty()) {
            CenteredMessage(
                icon = Icons.Filled.SearchOff,
                title = "No songs match “${state.query}”",
                body = "Try another word or clear the filter.",
                actionLabel = "Clear search",
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
                    )
                }
            }
        }
    }
}
