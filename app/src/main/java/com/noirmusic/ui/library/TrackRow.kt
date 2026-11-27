package com.noirmusic.ui.library

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.noirmusic.data.model.Track
import com.noirmusic.ui.common.formatDuration
import com.noirmusic.ui.common.spokenDuration
import com.noirmusic.ui.sakuga.LocalSakugaFx
import com.noirmusic.ui.sakuga.SakugaEqualizer
import com.noirmusic.ui.sakuga.SakugaIconButton
import com.noirmusic.ui.sakuga.SakugaIconVfx
import com.noirmusic.ui.sakuga.sakugaCelShadow
import com.noirmusic.ui.sakuga.sakugaTrackTint
import com.noirmusic.ui.sakuga.trackRootCenter
import com.noirmusic.ui.theme.SakugaTheme

@Composable
fun EqualizerGlyph(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    isPlaying: Boolean = true,
) {
    // Kept for backwards compatibility; renders the colorful Sakuga cel spectrum.
    SakugaEqualizer(isPlaying = isPlaying, modifier = modifier)
}

@Composable
fun TrackRow(
    track: Track,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
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
    val tokens = SakugaTheme.tokens
    val fx = LocalSakugaFx.current
    val durationText = if (track.durationMs > 0) formatDuration(track.durationMs) else "--:--"
    val a11yLabel = "${track.title}, ${track.displayArtist}, ${spokenDuration(track.durationMs)}" +
        if (isCurrent) ", now playing" else ""
    var menuExpanded by remember { mutableStateOf(false) }
    var rowCenter by remember { mutableStateOf(Offset.Zero) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val (badgeBg, badgeFg) = sakugaTrackTint(track.id)

    val scaleX by animateFloatAsState(
        targetValue = if (isPressed) 1.03f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 540f),
        label = "rowScaleX",
    )
    val scaleY by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 540f),
        label = "rowScaleY",
    )
    val shadowOff by animateDpAsState(
        targetValue = when {
            isDragging -> 7.dp
            isPressed -> 1.dp
            isCurrent -> 4.dp
            else -> 2.dp
        },
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 520f),
        label = "rowShadow",
    )

    val cardShape = RoundedCornerShape(12.dp)
    val cardBg = when {
        isDragging -> MaterialTheme.colorScheme.surfaceContainerHigh
        isCurrent -> MaterialTheme.colorScheme.surfaceContainer
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val borderColor = when {
        isDragging || isCurrent -> tokens.inkBorder
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val borderWidth = if (isCurrent || isDragging) 2.5.dp else 1.5.dp
    val shadowColor = when {
        isDragging -> tokens.accent
        isCurrent -> tokens.accent.copy(alpha = 0.78f)
        else -> tokens.celShadow.copy(alpha = 0.6f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .trackRootCenter { rowCenter = it }
            .graphicsLayer {
                this.scaleX = scaleX
                this.scaleY = scaleY
                rotationZ = if (isDragging) -1.2f else 0f
            }
            .sakugaCelShadow(
                color = shadowColor,
                cornerRadius = 12.dp,
                offsetX = shadowOff,
                offsetY = shadowOff + 1.dp,
            )
            .clip(cardShape)
            .background(cardBg, cardShape)
            .border(borderWidth, borderColor, cardShape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .semantics(mergeDescendants = true) { contentDescription = a11yLabel }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = {
                        if (rowCenter != Offset.Zero) {
                            fx.emitPrimaryConfirm(rowCenter, halfWDp = 76f, halfHDp = 20f, sfx = "ドン!")
                        }
                        onClick()
                    },
                )
                .padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Live anime equalizer or crisp cel track number badge
            if (isCurrent || trackIndex != null) {
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(28.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isCurrent) {
                        SakugaEqualizer(isPlaying = isPlaying)
                    } else if (trackIndex != null) {
                        Text(
                            text = String.format("%02d", trackIndex),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
            }

            // Colorful cel-framed track emblem
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .sakugaCelShadow(
                        color = tokens.celShadow.copy(alpha = 0.7f),
                        cornerRadius = 10.dp,
                        offsetX = 2.dp,
                        offsetY = 2.dp,
                    )
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isCurrent) tokens.accent else badgeBg)
                    .border(
                        width = 2.dp,
                        color = tokens.inkBorder,
                        shape = RoundedCornerShape(10.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = if (isCurrent) tokens.onAccent else badgeFg,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(Modifier.width(12.dp))

            // Track title & artist
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold,
                    fontStyle = if (isCurrent) FontStyle.Italic else FontStyle.Normal,
                    color = if (isCurrent) tokens.accentText else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
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
                )
            }

            if (isFavorite) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Lagu favorit",
                    tint = tokens.accentText,
                    modifier = Modifier.size(16.dp),
                )
            }

            Spacer(Modifier.width(6.dp))

            // Tabular duration stamp
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isCurrent) tokens.cyanFill
                        else MaterialTheme.colorScheme.surfaceContainer,
                    )
                    .border(
                        width = 1.dp,
                        color = if (isCurrent) tokens.inkBorder else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(6.dp),
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = durationText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrent) tokens.onCyanFill else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val hasMenuActions = onPlayNext != null ||
                onAddToQueue != null ||
                onToggleFavorite != null ||
                onMoveUp != null ||
                onMoveDown != null

            if (hasMenuActions) {
                Box {
                    SakugaIconButton(
                        onClick = { menuExpanded = true },
                        vfx = SakugaIconVfx.SNAP,
                        contentDescription = "Opsi lagu ${track.title}",
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        if (onPlayNext != null) {
                            DropdownMenuItem(
                                text = { Text("Putar Berikutnya", fontWeight = FontWeight.Bold) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
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
                                text = { Text("Tambahkan ke Antrean", fontWeight = FontWeight.Bold) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                        contentDescription = null,
                                        tint = tokens.cyan,
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
                                text = {
                                    Text(
                                        if (isFavorite) "Hapus dari Favorit" else "Tambah ke Favorit",
                                        fontWeight = FontWeight.Bold,
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                        contentDescription = null,
                                        tint = tokens.accentText,
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    if (rowCenter != Offset.Zero) {
                                        fx.emitFavoriteBurst(rowCenter, nowFavorite = !isFavorite)
                                    }
                                    onToggleFavorite()
                                },
                            )
                        }
                        if (onAddToPlaylist != null) {
                            DropdownMenuItem(
                                text = { Text("Tambah ke Playlist", fontWeight = FontWeight.Bold) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
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
                                text = { Text("Geser ke Atas", fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Filled.ArrowUpward, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onMoveUp()
                                },
                            )
                        }
                        if (onMoveDown != null) {
                            DropdownMenuItem(
                                text = { Text("Geser ke Bawah", fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Filled.ArrowDownward, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onMoveDown()
                                },
                            )
                        }
                        if (onRemoveFromPlaylist != null) {
                            DropdownMenuItem(
                                text = { Text("Hapus dari Playlist", fontWeight = FontWeight.Bold) },
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
                SakugaIconButton(
                    onClick = onRemoveFromPlaylist,
                    vfx = SakugaIconVfx.SNAP,
                    contentDescription = "Hapus dari playlist",
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                }
            } else if (onAddToPlaylist != null) {
                SakugaIconButton(
                    onClick = onAddToPlaylist,
                    vfx = SakugaIconVfx.SNAP,
                    contentDescription = "Tambah ke playlist",
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            if (dragHandleModifier != null) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .then(dragHandleModifier),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.DragHandle,
                        contentDescription = "Geser untuk mengurutkan",
                        tint = if (isDragging) tokens.accentText else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}
