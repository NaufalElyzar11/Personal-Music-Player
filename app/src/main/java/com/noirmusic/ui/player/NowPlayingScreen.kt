package com.noirmusic.ui.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.noirmusic.playback.SleepTimerState
import com.noirmusic.ui.common.formatDuration
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playback: PlaybackUiState,
    positionMsFlow: StateFlow<Long>,
    lyricsStateFlow: StateFlow<LyricsUiState>,
    activeLineIndexFlow: StateFlow<Int>,
    artworkFlow: StateFlow<androidx.compose.ui.graphics.ImageBitmap?>,
    sleepTimerStateFlow: StateFlow<SleepTimerState>,
    pane: NowPlayingPane,
    onPaneChange: (NowPlayingPane) -> Unit,
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onRetryLyrics: () -> Unit,
    onStartSleepTimer: (Int) -> Unit,
    onStartSleepTimerEndOfTrack: () -> Unit,
    onAddSleepTimerMinutes: (Int) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onAddToPlaylist: (() -> Unit)? = null,
    onSkipToQueueItem: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val track = playback.nowPlaying
    val sleepTimerState by sleepTimerStateFlow.collectAsStateWithLifecycle()
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showQueueDialog by remember { mutableStateOf(false) }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            state = sleepTimerState,
            onStartTimer = onStartSleepTimer,
            onStartEndOfTrack = onStartSleepTimerEndOfTrack,
            onAddMinutes = onAddSleepTimerMinutes,
            onCancelTimer = onCancelSleepTimer,
            onDismiss = { showSleepTimerDialog = false },
        )
    }

    if (showQueueDialog) {
        PlaybackQueueDialog(
            playback = playback,
            onSkipToQueueItem = onSkipToQueueItem,
            onDismiss = { showQueueDialog = false },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(bottom = 16.dp),
    ) {
        // 1. Top Navigation Bar (Studio Minimalist Header)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Tutup pemutar musik",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp),
                )
            }

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "MEMUTAR DARI LIBRARY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = track?.album ?: "Noir Hi-Fi",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (playback.queueSize > 0) {
                    IconButton(onClick = { showQueueDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Buka antrean",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                if (onAddToPlaylist != null && track != null) {
                    IconButton(onClick = onAddToPlaylist) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                            contentDescription = "Tambah ke playlist",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }

        // 2. Cover / Lyrics Tactile Switch
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.widthIn(max = 220.dp)) {
                SegmentedButton(
                    selected = pane == NowPlayingPane.COVER,
                    onClick = { onPaneChange(NowPlayingPane.COVER) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        activeContentColor = MaterialTheme.colorScheme.primary,
                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) {
                    Text("Cover", fontWeight = if (pane == NowPlayingPane.COVER) FontWeight.SemiBold else FontWeight.Normal)
                }
                SegmentedButton(
                    selected = pane == NowPlayingPane.LYRICS,
                    onClick = { onPaneChange(NowPlayingPane.LYRICS) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        activeContentColor = MaterialTheme.colorScheme.primary,
                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) {
                    Text("Lirik", fontWeight = if (pane == NowPlayingPane.LYRICS) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }

        // 3. Central Content Area (Artwork / Lyrics)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            if (pane == NowPlayingPane.COVER) {
                CoverPane(
                    artworkFlow = artworkFlow,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LyricsPane(
                    lyricsStateFlow = lyricsStateFlow,
                    activeLineIndexFlow = activeLineIndexFlow,
                    onSeekTo = onSeekTo,
                    onRetry = onRetryLyrics,
                )
            }
        }

        // 4. Title & Artist Info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp),
        ) {
            Text(
                text = track?.title ?: "Tidak ada lagu",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = track?.displayArtist ?: "",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(8.dp))

        // 5. Studio Scrubber / Seek Section with Tabular Monospace Timestamps
        SeekSection(
            positionMsFlow = positionMsFlow,
            durationMs = playback.durationMs,
            onSeekTo = onSeekTo,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        )

        if (playback.errorMessage != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "File tidak dapat diputar · ${playback.errorMessage}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }

        Spacer(Modifier.height(8.dp))

        // 6. Transport Controls (Studio Ergonomics)
        ControlsRow(
            playback = playback,
            onTogglePlayPause = onTogglePlayPause,
            onPrevious = onPrevious,
            onNext = onNext,
            onToggleShuffle = onToggleShuffle,
            onCycleRepeat = onCycleRepeat,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(8.dp))

        // 7. Bottom Auxiliary Bar (Sleep Timer Badge & Telemetry)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Sleep Timer Pill
            val isTimerActive = sleepTimerState is SleepTimerState.Active
            val timerText = when (val s = sleepTimerState) {
                is SleepTimerState.Active -> {
                    if (s.isEndOfTrack) "Tidur: Akhir lagu"
                    else {
                        val m = s.remainingSec / 60
                        val sec = s.remainingSec % 60
                        String.format(Locale.getDefault(), "Tidur: %02d:%02d", m, sec)
                    }
                }
                else -> "Timer Tidur"
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isTimerActive) MaterialTheme.colorScheme.surfaceContainerHigh
                        else MaterialTheme.colorScheme.surfaceContainerLow,
                    )
                    .border(
                        width = 1.dp,
                        color = if (isTimerActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(6.dp),
                    )
                    .clickable { showSleepTimerDialog = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Bedtime,
                    contentDescription = "Timer tidur",
                    tint = if (isTimerActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = timerText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isTimerActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isTimerActive) FontWeight.SemiBold else FontWeight.Medium,
                )
            }

            // Queue count indicator / tactile chip
            if (playback.queueSize > 0) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.clickable { showQueueDialog = true },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Buka antrean",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = "${playback.queueIndex + 1} / ${playback.queueSize}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeekSection(
    positionMsFlow: StateFlow<Long>,
    durationMs: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val positionMs by positionMsFlow.collectAsStateWithLifecycle()
    val enabled = durationMs > 0L
    var isDragging by remember { mutableStateOf(false) }
    var dragMs by remember { mutableFloatStateOf(0f) }

    val shownMs = if (isDragging) {
        dragMs.roundToLong()
    } else {
        positionMs.coerceIn(0L, durationMs.coerceAtLeast(0L))
    }
    val elapsedText = formatDuration(shownMs)
    val remainingMs = (durationMs - shownMs).coerceAtLeast(0L)
    val remainingText = if (enabled) "-${formatDuration(remainingMs)}" else "--:--"

    Column(modifier = modifier) {
        Slider(
            value = if (enabled) shownMs.toFloat() else 0f,
            onValueChange = {
                isDragging = true
                dragMs = it
            },
            onValueChangeFinished = {
                val target = dragMs.roundToLong()
                isDragging = false
                onSeekTo(target)
            },
            valueRange = 0f..(durationMs.coerceAtLeast(1L).toFloat()),
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Seek"
                    stateDescription = "$elapsedText dari $remainingText"
                },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = elapsedText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = remainingText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ControlsRow(
    playback: PlaybackUiState,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val repeatDescription = when (playback.repeatMode) {
        Player.REPEAT_MODE_ALL -> "Repeat all"
        Player.REPEAT_MODE_ONE -> "Repeat one"
        else -> "Repeat off"
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Shuffle with active dot indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(48.dp),
        ) {
            IconButton(
                onClick = onToggleShuffle,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = if (playback.shuffleEnabled) "Shuffle on" else "Shuffle off",
                    tint = if (playback.shuffleEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(22.dp),
                )
            }
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(
                        if (playback.shuffleEnabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.background,
                    ),
            )
        }

        // Previous track
        IconButton(
            onClick = onPrevious,
            enabled = playback.hasPrevious,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = "Previous track",
                tint = if (playback.hasPrevious) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(26.dp),
            )
        }

        // Tactile Studio Knob Play/Pause button
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape,
                )
                .clickable(enabled = playback.queueSize > 0, onClick = onTogglePlayPause),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        if (playback.isPlaying) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (playback.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playback.isPlaying) "Pause" else "Play",
                    tint = if (playback.isPlaying) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(26.dp),
                )
            }
            if (playback.isBuffering) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(58.dp),
                )
            }
        }

        // Next track
        IconButton(
            onClick = onNext,
            enabled = playback.hasNext || playback.repeatMode != Player.REPEAT_MODE_OFF,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Next track",
                tint = if (playback.hasNext || playback.repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(26.dp),
            )
        }

        // Repeat with active dot indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(48.dp),
        ) {
            IconButton(
                onClick = onCycleRepeat,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    imageVector = if (playback.repeatMode == Player.REPEAT_MODE_ONE) {
                        Icons.Filled.RepeatOne
                    } else {
                        Icons.Filled.Repeat
                    },
                    contentDescription = repeatDescription,
                    tint = if (playback.repeatMode != Player.REPEAT_MODE_OFF) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(22.dp),
                )
            }
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(
                        if (playback.repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.background,
                    ),
            )
        }
    }
}

@Composable
private fun CoverPane(
    artworkFlow: StateFlow<androidx.compose.ui.graphics.ImageBitmap?>,
    modifier: Modifier = Modifier,
) {
    val artwork by artworkFlow.collectAsStateWithLifecycle()
    Artwork(bitmap = artwork, modifier = modifier)
}

@Composable
private fun LyricsPane(
    lyricsStateFlow: StateFlow<LyricsUiState>,
    activeLineIndexFlow: StateFlow<Int>,
    onSeekTo: (Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lyricsState by lyricsStateFlow.collectAsStateWithLifecycle()
    val activeLineIndex by activeLineIndexFlow.collectAsStateWithLifecycle()
    LyricsPanel(
        state = lyricsState,
        activeLineIndex = activeLineIndex,
        onSeekTo = onSeekTo,
        onRetry = onRetry,
        modifier = modifier,
    )
}
