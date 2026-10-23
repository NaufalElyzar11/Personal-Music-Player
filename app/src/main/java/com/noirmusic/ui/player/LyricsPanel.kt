package com.noirmusic.ui.player

import android.os.SystemClock
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.noirmusic.data.lyrics.LyricLine
import com.noirmusic.data.lyrics.LyricsError
import com.noirmusic.ui.common.CenteredMessage
import com.noirmusic.ui.common.LoadingView
import com.noirmusic.ui.theme.LyricLineStyle

private const val USER_SCROLL_COOLDOWN_MS = 3_000L

@Composable
fun LyricsPanel(
    state: LyricsUiState,
    activeLineIndex: Int,
    onSeekTo: (Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (state) {
            LyricsUiState.NoTrack -> CenteredMessage(
                icon = Icons.Filled.Lyrics,
                title = "No song selected",
                body = "Pick a song to see its lyrics.",
            )
            LyricsUiState.Loading -> LoadingView("Looking up lyrics…")
            is LyricsUiState.Synced -> SyncedLyricsView(
                lines = state.lines,
                activeIndex = activeLineIndex,
                onSeekTo = onSeekTo,
            )
            is LyricsUiState.Plain -> PlainLyricsView(lines = state.lines)
            LyricsUiState.Instrumental -> CenteredMessage(
                icon = Icons.Filled.MusicNote,
                title = "Instrumental",
                body = "No lyrics for this track.",
            )
            LyricsUiState.NotFound -> CenteredMessage(
                icon = Icons.Filled.Lyrics,
                title = "No lyrics found",
                body = "LRCLIB doesn't have lyrics for this song yet.",
            )
            LyricsUiState.NotSearchable -> CenteredMessage(
                icon = Icons.Filled.Lyrics,
                title = "Can't look up lyrics",
                body = "This file is missing title/artist tags.",
            )
            is LyricsUiState.Error -> LyricsErrorView(error = state.error, onRetry = onRetry)
        }
    }
}

@Composable
private fun SyncedLyricsView(
    lines: List<LyricLine>,
    activeIndex: Int,
    onSeekTo: (Long) -> Unit,
) {
    val listState = rememberLazyListState()
    val isDragged by listState.interactionSource.collectIsDraggedAsState()
    var lastUserDragMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isDragged) {
        if (!isDragged && lastUserDragMs != 0L) {
            lastUserDragMs = SystemClock.elapsedRealtime()
        } else if (isDragged) {
            lastUserDragMs = SystemClock.elapsedRealtime()
        }
    }

    LaunchedEffect(activeIndex, lines) {
        if (activeIndex in lines.indices && !isDragged &&
            SystemClock.elapsedRealtime() - lastUserDragMs >= USER_SCROLL_COOLDOWN_MS
        ) {
            runCatching { listState.animateScrollToItem(activeIndex) }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val verticalPad = maxHeight * 0.4f
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(vertical = verticalPad),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(
                items = lines,
                key = { index, _ -> index },
                contentType = { _, _ -> "lyric" },
            ) { index, line ->
                val isActive = index == activeIndex
                val color by animateColorAsState(
                    targetValue = if (isActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    },
                    label = "lyricColor",
                )
                Text(
                    text = line.text.ifEmpty { "♪" },
                    style = LyricLineStyle,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                    color = color,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Seek to line",
                            onClick = { onSeekTo(line.timeMs) },
                        )
                        .padding(vertical = 8.dp),
                )
            }
            item(key = "attribution") {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Lyrics by LRCLIB",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PlainLyricsView(lines: List<String>) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "header") {
            Text(
                text = "Not synced",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
        itemsIndexed(
            items = lines,
            key = { index, _ -> index },
            contentType = { _, _ -> "plain" },
        ) { _, line ->
            if (line.isEmpty()) {
                Spacer(Modifier.height(16.dp))
            } else {
                Text(
                    text = line,
                    style = LyricLineStyle,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                )
            }
        }
        item(key = "attribution") {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Lyrics by LRCLIB",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LyricsErrorView(error: LyricsError, onRetry: () -> Unit) {
    val (icon, title, body) = when (error) {
        LyricsError.Offline -> Triple(
            Icons.Filled.CloudOff,
            "You're offline",
            "Playback still works. Connect to load lyrics.",
        )
        LyricsError.Timeout -> Triple(
            Icons.Filled.HourglassEmpty,
            "Timed out",
            "LRCLIB took too long to respond.",
        )
        is LyricsError.RateLimited -> Triple(
            Icons.Filled.HourglassEmpty,
            "Slow down",
            "LRCLIB asked us to slow down. Try again in about ${error.retryAfterSec} s.",
        )
        is LyricsError.Server -> Triple(
            Icons.Filled.ErrorOutline,
            "Server error",
            "LRCLIB is having trouble (HTTP ${error.code}).",
        )
        is LyricsError.Http -> Triple(
            Icons.Filled.ErrorOutline,
            "Request failed",
            "Lyrics request failed (HTTP ${error.code}).",
        )
        LyricsError.Malformed -> Triple(
            Icons.Filled.ErrorOutline,
            "Unreadable response",
            "Received an unreadable response.",
        )
        LyricsError.Network -> Triple(
            Icons.Filled.ErrorOutline,
            "Network error",
            "Couldn't reach LRCLIB.",
        )
    }
    Column(modifier = Modifier.fillMaxSize()) {
        CenteredMessage(
            icon = icon,
            title = title,
            body = body,
            actionLabel = "Retry",
            onAction = onRetry,
        )
    }
}
