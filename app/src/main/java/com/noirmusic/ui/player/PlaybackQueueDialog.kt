package com.noirmusic.ui.player

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.noirmusic.playback.PlayerLogic
import com.noirmusic.ui.common.CenteredMessage
import com.noirmusic.ui.common.dragHandle
import com.noirmusic.ui.common.formatDuration
import com.noirmusic.ui.common.rememberDragReorderState
import com.noirmusic.ui.library.EqualizerGlyph

private data class QueueRowEntry(
    val uid: String,
    val item: NowPlaying,
)

private fun buildQueueEntries(queue: List<NowPlaying>): List<QueueRowEntry> {
    val counts = HashMap<String, Int>(queue.size)
    return queue.map { item ->
        val occ = counts.getOrDefault(item.mediaId, 0)
        counts[item.mediaId] = occ + 1
        QueueRowEntry(uid = "${item.mediaId}#$occ", item = item)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackQueueDialog(
    playback: PlaybackUiState,
    onSkipToQueueItem: (Int) -> Unit,
    onMoveQueueItem: (fromIndex: Int, toIndex: Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
    onClearUpcomingQueue: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayQueue = remember { mutableStateListOf<QueueRowEntry>() }
    val listState = rememberLazyListState()
    val dragState = rememberDragReorderState(
        listState = listState,
        onMove = { from, to ->
            if (from in displayQueue.indices && to in displayQueue.indices) {
                displayQueue.add(to, displayQueue.removeAt(from))
            }
        },
        onCommitMove = { from, to ->
            onMoveQueueItem(from, to)
        },
    )

    LaunchedEffect(playback.queue, dragState.draggedKey) {
        if (dragState.draggedKey == null) {
            displayQueue.clear()
            displayQueue.addAll(buildQueueEntries(playback.queue))
        }
    }

    val activeUid = remember(playback.queue, playback.queueIndex) {
        buildQueueEntries(playback.queue).getOrNull(playback.queueIndex)?.uid
    }
    val canClearUpcoming = PlayerLogic.canClearUpcoming(playback.queueIndex, playback.queueSize)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.94f)
                .heightIn(min = 260.dp, max = 580.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 8.dp, top = 14.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Antrean Putar",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${playback.queueSize} lagu · Geser untuk mengatur urutan",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (canClearUpcoming) {
                        TextButton(onClick = onClearUpcomingQueue) {
                            Icon(
                                imageVector = Icons.Filled.ClearAll,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Kosongkan")
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Tutup",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Current track summary
                val current = playback.nowPlaying
                if (current != null) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = "SEDANG DIPUTAR",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = current.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = current.displayArtist,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                EqualizerGlyph(modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 0.5.dp,
                )

                // Reorderable queue list
                if (displayQueue.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                    ) {
                        CenteredMessage(
                            icon = Icons.AutoMirrored.Filled.QueueMusic,
                            title = "Antrean kosong",
                            body = "Putar lagu atau pilih Tambahkan ke Antrean dari daftar lagu.",
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(max = 360.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        itemsIndexed(
                            items = displayQueue,
                            key = { _, entry -> entry.uid },
                        ) { index, entry ->
                            val isItemCurrent = entry.uid == activeUid
                            val isDragging = dragState.draggedKey == entry.uid
                            var rowMenuOpen by remember { mutableStateOf(false) }
                            val dismissState = rememberSwipeToDismissBoxState()

                            LaunchedEffect(dismissState.currentValue) {
                                if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                                    val idx = displayQueue.indexOfFirst { it.uid == entry.uid }
                                    if (idx >= 0) onRemoveQueueItem(idx)
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
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.errorContainer)
                                            .padding(horizontal = 16.dp),
                                        contentAlignment = Alignment.CenterEnd,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Hapus dari antrean",
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                        )
                                    }
                                },
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = when {
                                        isDragging -> MaterialTheme.colorScheme.surfaceContainerHighest
                                        isItemCurrent -> MaterialTheme.colorScheme.surfaceContainerHigh
                                        else -> MaterialTheme.colorScheme.surfaceContainer
                                    },
                                    border = if (isDragging) {
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                    } else {
                                        null
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSkipToQueueItem(index) },
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = String.format("%02d", index + 1),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isItemCurrent) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outline
                                            },
                                            modifier = Modifier.width(26.dp),
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = entry.item.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isItemCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (isItemCurrent) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                },
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = entry.item.displayArtist,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = formatDuration(entry.item.durationMs),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Box {
                                            IconButton(onClick = { rowMenuOpen = true }) {
                                                Icon(
                                                    imageVector = Icons.Filled.MoreVert,
                                                    contentDescription = "Opsi antrean ${entry.item.title}",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = rowMenuOpen,
                                                onDismissRequest = { rowMenuOpen = false },
                                            ) {
                                                if (index > 0) {
                                                    DropdownMenuItem(
                                                        text = { Text("Naikkan") },
                                                        leadingIcon = {
                                                            Icon(Icons.Filled.ArrowUpward, contentDescription = null)
                                                        },
                                                        onClick = {
                                                            rowMenuOpen = false
                                                            onMoveQueueItem(index, index - 1)
                                                        },
                                                    )
                                                }
                                                if (index < displayQueue.lastIndex) {
                                                    DropdownMenuItem(
                                                        text = { Text("Turunkan") },
                                                        leadingIcon = {
                                                            Icon(Icons.Filled.ArrowDownward, contentDescription = null)
                                                        },
                                                        onClick = {
                                                            rowMenuOpen = false
                                                            onMoveQueueItem(index, index + 1)
                                                        },
                                                    )
                                                }
                                                DropdownMenuItem(
                                                    text = { Text("Hapus dari antrean") },
                                                    leadingIcon = {
                                                        Icon(
                                                            Icons.Filled.Delete,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.error,
                                                        )
                                                    },
                                                    onClick = {
                                                        rowMenuOpen = false
                                                        onRemoveQueueItem(index)
                                                    },
                                                )
                                            }
                                        }
                                        if (displayQueue.size > 1) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .dragHandle(dragState, entry.uid),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.DragHandle,
                                                    contentDescription = "Geser urutan antrean",
                                                    tint = if (isDragging) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    },
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 0.5.dp,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Tutup")
                    }
                }
            }
        }
    }
}
