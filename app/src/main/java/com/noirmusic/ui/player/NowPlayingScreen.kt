package com.noirmusic.ui.player

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.noirmusic.ui.common.formatDuration
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playback: PlaybackUiState,
    positionMsFlow: StateFlow<Long>,
    lyricsStateFlow: StateFlow<LyricsUiState>,
    activeLineIndexFlow: StateFlow<Int>,
    artworkFlow: StateFlow<androidx.compose.ui.graphics.ImageBitmap?>,
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
    modifier: Modifier = Modifier,
) {
    val track = playback.nowPlaying

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(bottom = 24.dp),
    ) {
        // 1. Top row
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
                    contentDescription = "Close player",
                )
            }
            Text(
                text = "Now playing",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // 2. Cover / Lyrics switch
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.widthIn(max = 240.dp)) {
                SegmentedButton(
                    selected = pane == NowPlayingPane.COVER,
                    onClick = { onPaneChange(NowPlayingPane.COVER) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) {
                    Text("Cover")
                }
                SegmentedButton(
                    selected = pane == NowPlayingPane.LYRICS,
                    onClick = { onPaneChange(NowPlayingPane.LYRICS) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) {
                    Text("Lyrics")
                }
            }
        }

        // 3. Content area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
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

        // 4. Title & artist
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            Text(
                text = track?.title ?: "Nothing playing",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = track?.displayArtist ?: "",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(12.dp))

        // 5. Seek bar (only this composable collects positionMs)
        SeekSection(
            positionMsFlow = positionMsFlow,
            durationMs = playback.durationMs,
            onSeekTo = onSeekTo,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        )

        if (playback.errorMessage != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Can't play this file · ${playback.errorMessage}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }

        Spacer(Modifier.height(12.dp))

        // 6. Controls row
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
    val totalText = if (enabled) formatDuration(durationMs) else "--:--"

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
                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Seek"
                    stateDescription = "$elapsedText of $totalText"
                },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = elapsedText,
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = totalText,
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
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
        IconButton(
            onClick = onToggleShuffle,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Shuffle,
                contentDescription = if (playback.shuffleEnabled) "Shuffle on" else "Shuffle off",
                tint = if (playback.shuffleEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }

        IconButton(
            onClick = onPrevious,
            enabled = playback.hasPrevious,
            modifier = Modifier.size(56.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = "Previous track",
                modifier = Modifier.size(32.dp),
            )
        }

        Box(contentAlignment = Alignment.Center) {
            FilledIconButton(
                onClick = onTogglePlayPause,
                enabled = playback.queueSize > 0,
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier.size(72.dp),
            ) {
                Icon(
                    imageVector = if (playback.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playback.isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(36.dp),
                )
            }
            if (playback.isBuffering) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(64.dp),
                )
            }
        }

        IconButton(
            onClick = onNext,
            enabled = playback.hasNext || playback.repeatMode != Player.REPEAT_MODE_OFF,
            modifier = Modifier.size(56.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Next track",
                modifier = Modifier.size(32.dp),
            )
        }

        IconButton(
            onClick = onCycleRepeat,
            modifier = Modifier.size(48.dp),
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
