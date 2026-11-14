package com.noirmusic.ui.library

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.noirmusic.data.model.Track
import com.noirmusic.ui.common.formatDuration
import com.noirmusic.ui.common.spokenDuration

@Composable
fun EqualizerGlyph(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "eq")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "h1",
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 16f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "h2",
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "h3",
    )

    Row(
        modifier = modifier.size(18.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(Modifier.width(3.dp).height(h1.dp).clip(RoundedCornerShape(topStart = 1.dp, topEnd = 1.dp)).background(color))
        Box(Modifier.width(3.dp).height(h2.dp).clip(RoundedCornerShape(topStart = 1.dp, topEnd = 1.dp)).background(color))
        Box(Modifier.width(3.dp).height(h3.dp).clip(RoundedCornerShape(topStart = 1.dp, topEnd = 1.dp)).background(color))
    }
}

@Composable
fun TrackRow(
    track: Track,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trackIndex: Int? = null,
    isFavorite: Boolean = false,
    subtitleSuffix: String? = null,
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    dragHandleModifier: Modifier? = null,
    isDragging: Boolean = false,
) {
    val durationText = if (track.durationMs > 0) formatDuration(track.durationMs) else "--:--"
    val a11yLabel = "${track.title}, ${track.displayArtist}, ${spokenDuration(track.durationMs)}" +
        if (isCurrent) ", now playing" else ""
    var menuExpanded by remember { mutableStateOf(false) }

    val rowBackground = if (isDragging) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        Color.Transparent
    }
    val rowModifier = if (isDragging) {
        modifier
            .fillMaxWidth()
            .background(rowBackground)
            .border(1.dp, MaterialTheme.colorScheme.primary)
    } else {
        modifier
            .fillMaxWidth()
            .background(rowBackground)
    }

    Column(modifier = rowModifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp)
                .semantics(mergeDescendants = true) { contentDescription = a11yLabel }
                .clickable(role = Role.Button, onClick = onClick)
                .padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Index or Live EQ Indicator
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(28.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isCurrent) {
                    EqualizerGlyph(color = MaterialTheme.colorScheme.primary)
                } else if (trackIndex != null) {
                    Text(
                        text = String.format("%02d", trackIndex),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            // Thumbnail
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(
                        width = 1.dp,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(6.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(Modifier.width(12.dp))

            // Track title & artist
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val subtitle = if (subtitleSuffix.isNullOrBlank()) {
                        track.displayArtist
                    } else {
                        "${track.displayArtist} · $subtitleSuffix"
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }

            if (isFavorite) {
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Lagu favorit",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                )
            }

            Spacer(Modifier.width(8.dp))

            // Duration in tabular monospace
            Text(
                text = durationText,
                style = MaterialTheme.typography.labelMedium,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Options action
            val hasMenuActions = onPlayNext != null ||
                onAddToQueue != null ||
                onToggleFavorite != null ||
                onMoveUp != null ||
                onMoveDown != null

            if (hasMenuActions) {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Opsi lagu ${track.title}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        if (onPlayNext != null) {
                            DropdownMenuItem(
                                text = { Text("Putar Berikutnya") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onPlayNext()
                                },
                            )
                        }
                        if (onAddToQueue != null) {
                            DropdownMenuItem(
                                text = { Text("Tambahkan ke Antrean") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onAddToQueue()
                                },
                            )
                        }
                        if (onToggleFavorite != null) {
                            DropdownMenuItem(
                                text = { Text(if (isFavorite) "Hapus dari Favorit" else "Tambah ke Favorit") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                        contentDescription = null,
                                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onToggleFavorite()
                                },
                            )
                        }
                        if (onAddToPlaylist != null) {
                            DropdownMenuItem(
                                text = { Text("Tambah ke Playlist") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onAddToPlaylist()
                                },
                            )
                        }
                        if (onMoveUp != null) {
                            DropdownMenuItem(
                                text = { Text("Geser ke Atas") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowUpward,
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onMoveUp()
                                },
                            )
                        }
                        if (onMoveDown != null) {
                            DropdownMenuItem(
                                text = { Text("Geser ke Bawah") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDownward,
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onMoveDown()
                                },
                            )
                        }
                        if (onRemoveFromPlaylist != null) {
                            DropdownMenuItem(
                                text = { Text("Hapus dari Playlist") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onRemoveFromPlaylist()
                                },
                            )
                        }
                    }
                }
            } else if (onRemoveFromPlaylist != null) {
                IconButton(onClick = onRemoveFromPlaylist) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Hapus dari playlist",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            } else if (onAddToPlaylist != null) {
                IconButton(onClick = onAddToPlaylist) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                        contentDescription = "Tambah ke playlist",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            if (dragHandleModifier != null) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .then(dragHandleModifier),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.DragHandle,
                        contentDescription = "Geser untuk mengurutkan",
                        tint = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            thickness = 0.5.dp,
            modifier = Modifier.padding(start = 54.dp),
        )
    }
}
