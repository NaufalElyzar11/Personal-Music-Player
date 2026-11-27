package com.noirmusic.ui.player

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontStyle
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
import com.noirmusic.ui.sakuga.SakugaButton
import com.noirmusic.ui.sakuga.SakugaButtonStyle
import com.noirmusic.ui.sakuga.SakugaDialog
import com.noirmusic.ui.sakuga.SakugaEqualizer
import com.noirmusic.ui.sakuga.SakugaIconButton
import com.noirmusic.ui.sakuga.SakugaIconVfx
import com.noirmusic.ui.sakuga.SakugaPanel
import com.noirmusic.ui.sakuga.sakugaCelShadow
import com.noirmusic.ui.theme.SakugaTheme

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
    val tokens = SakugaTheme.tokens
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

    SakugaDialog(onDismissRequest = onDismiss, horizontalPadding = 14.dp) {
        SakugaPanel(
                    modifier = modifier
                        .fillMaxWidth()
                        .heightIn(min = 260.dp, max = 600.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    shadowColor = tokens.celShadow,
                    cornerRadius = 18.dp,
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Antrean Putar",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "${playback.queueSize} lagu · Geser untuk atur urutan",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (canClearUpcoming) {
                                SakugaButton(
                                    onClick = onClearUpcomingQueue,
                                    style = SakugaButtonStyle.SECONDARY,
                                    cornerRadius = 10.dp,
                                    sfxText = "ズザッ",
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                ) {
                                    Icon(Icons.Filled.ClearAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Kosongkan", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                                }
                            }
                            SakugaIconButton(
                                onClick = onDismiss,
                                vfx = SakugaIconVfx.SNAP,
                                contentDescription = "Tutup",
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }

                        // Current Track Highlight Card
                        val current = playback.nowPlaying
                        if (current != null) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(tokens.accent)
                                        .border(1.5.dp, tokens.inkBorder, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 7.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = "SEDANG DIPUTAR",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        fontStyle = FontStyle.Italic,
                                        color = tokens.onAccent,
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .sakugaCelShadow(tokens.accent.copy(alpha = 0.7f), cornerRadius = 12.dp, offsetX = 3.dp, offsetY = 3.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainer)
                                        .border(2.dp, tokens.inkBorder, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = current.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Black,
                                            fontStyle = FontStyle.Italic,
                                            color = tokens.accentText,
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
                                    SakugaEqualizer(isPlaying = playback.isPlaying)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }

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
                                    .heightIn(max = 340.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
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
                                            rotationZ = if (isDragging) -1f else 0f
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
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(MaterialTheme.colorScheme.errorContainer)
                                                    .border(2.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(10.dp))
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
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    when {
                                                        isDragging -> MaterialTheme.colorScheme.surfaceContainerHighest
                                                        isItemCurrent -> MaterialTheme.colorScheme.surfaceContainer
                                                        else -> MaterialTheme.colorScheme.surfaceContainerLow
                                                    },
                                                )
                                                .border(
                                                    width = if (isDragging || isItemCurrent) 2.dp else 1.5.dp,
                                                    color = if (isDragging || isItemCurrent) tokens.inkBorder else MaterialTheme.colorScheme.outlineVariant,
                                                    shape = RoundedCornerShape(10.dp),
                                                )
                                                .clickable { onSkipToQueueItem(index) }
                                                .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text = String.format("%02d", index + 1),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Black,
                                                color = if (isItemCurrent) tokens.accentText else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.width(26.dp),
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = entry.item.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isItemCurrent) FontWeight.Black else FontWeight.Bold,
                                                    color = if (isItemCurrent) tokens.accentText else MaterialTheme.colorScheme.onSurface,
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
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = formatDuration(entry.item.durationMs),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            Box {
                                                SakugaIconButton(
                                                    onClick = { rowMenuOpen = true },
                                                    vfx = SakugaIconVfx.SNAP,
                                                    contentDescription = "Opsi antrean ${entry.item.title}",
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.MoreVert,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.size(18.dp),
                                                    )
                                                }
                                                DropdownMenu(
                                                    expanded = rowMenuOpen,
                                                    onDismissRequest = { rowMenuOpen = false },
                                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                ) {
                                                    if (index > 0) {
                                                        DropdownMenuItem(
                                                            text = { Text("Naikkan", fontWeight = FontWeight.Bold) },
                                                            leadingIcon = { Icon(Icons.Filled.ArrowUpward, contentDescription = null) },
                                                            onClick = {
                                                                rowMenuOpen = false
                                                                onMoveQueueItem(index, index - 1)
                                                            },
                                                        )
                                                    }
                                                    if (index < displayQueue.lastIndex) {
                                                        DropdownMenuItem(
                                                            text = { Text("Turunkan", fontWeight = FontWeight.Bold) },
                                                            leadingIcon = { Icon(Icons.Filled.ArrowDownward, contentDescription = null) },
                                                            onClick = {
                                                                rowMenuOpen = false
                                                                onMoveQueueItem(index, index + 1)
                                                            },
                                                        )
                                                    }
                                                    DropdownMenuItem(
                                                        text = { Text("Hapus dari antrean", fontWeight = FontWeight.Bold) },
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
                                                        tint = if (isDragging) tokens.accentText else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(20.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            SakugaButton(
                                onClick = onDismiss,
                                style = SakugaButtonStyle.SECONDARY,
                                cornerRadius = 10.dp,
                            ) {
                                Text("Tutup", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
    }
}
