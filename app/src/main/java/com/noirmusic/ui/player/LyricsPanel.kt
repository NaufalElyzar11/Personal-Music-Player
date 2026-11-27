package com.noirmusic.ui.player

import android.os.SystemClock
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.noirmusic.data.lyrics.LyricLine
import com.noirmusic.data.lyrics.LyricsError
import com.noirmusic.ui.common.CenteredMessage
import com.noirmusic.ui.common.LoadingView
import com.noirmusic.ui.sakuga.LocalSakugaFx
import com.noirmusic.ui.sakuga.sakugaCelShadow
import com.noirmusic.ui.sakuga.trackRootCenter
import com.noirmusic.ui.theme.ActiveLyricLineStyle
import com.noirmusic.ui.theme.LyricLineStyle
import com.noirmusic.ui.theme.SakugaTheme

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
                title = "Belum ada lagu dipilih",
                body = "Pilih lagu untuk menampilkan liriknya.",
            )
            LyricsUiState.Loading -> LoadingView("Mencari lirik lagu...")
            is LyricsUiState.Synced -> SyncedLyricsView(
                lines = state.lines,
                activeIndex = activeLineIndex,
                onSeekTo = onSeekTo,
            )
            is LyricsUiState.Plain -> PlainLyricsView(lines = state.lines)
            LyricsUiState.Instrumental -> CenteredMessage(
                icon = Icons.Filled.MusicNote,
                title = "Instrumental",
                body = "Lagu ini tidak memiliki lirik vokal.",
            )
            LyricsUiState.NotFound -> CenteredMessage(
                icon = Icons.Filled.Lyrics,
                title = "Lirik tidak ditemukan",
                body = "LRCLIB belum memiliki lirik untuk lagu ini.",
            )
            LyricsUiState.NotSearchable -> CenteredMessage(
                icon = Icons.Filled.Lyrics,
                title = "Tidak bisa mencari lirik",
                body = "Tag judul atau artis pada file audio belum lengkap.",
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
    val tokens = SakugaTheme.tokens
    val fx = LocalSakugaFx.current
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
        val verticalPad = maxHeight * 0.36f
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(vertical = verticalPad, horizontal = 4.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(
                items = lines,
                key = { index, _ -> index },
                contentType = { _, _ -> "lyric" },
            ) { index, line ->
                val isActive = index == activeIndex
                var lineCenter by remember { mutableStateOf(Offset.Zero) }

                val textColor by animateColorAsState(
                    targetValue = if (isActive) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    label = "lyricColor",
                )
                val popScale by animateFloatAsState(
                    targetValue = if (isActive) 1.03f else 0.96f,
                    animationSpec = spring(dampingRatio = 0.48f, stiffness = 420f),
                    label = "lyricPop",
                )

                val containerModifier = if (isActive) {
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .graphicsLayer {
                            scaleX = popScale
                            scaleY = popScale
                        }
                        .sakugaCelShadow(tokens.accent, cornerRadius = 14.dp, offsetX = 4.dp, offsetY = 5.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .border(2.5.dp, tokens.inkBorder, RoundedCornerShape(14.dp))
                } else {
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                }

                Box(
                    modifier = containerModifier
                        .trackRootCenter { lineCenter = it }
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Lompat ke baris lirik",
                            onClick = {
                                if (lineCenter != Offset.Zero) {
                                    fx.emitSecondarySnap(lineCenter, sfx = "カチッ")
                                }
                                onSeekTo(line.timeMs)
                            },
                        )
                        .padding(horizontal = if (isActive) 14.dp else 8.dp, vertical = if (isActive) 12.dp else 7.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isActive) {
                            Box(
                                modifier = Modifier
                                    .width(5.dp)
                                    .height(26.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(tokens.accent),
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                        Text(
                            text = line.text.ifEmpty { "♪" },
                            style = if (isActive) ActiveLyricLineStyle else LyricLineStyle,
                            color = textColor,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            item(key = "attribution") {
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "Lirik oleh LRCLIB",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun PlainLyricsView(lines: List<String>) {
    val tokens = SakugaTheme.tokens
    LazyColumn(
        contentPadding = PaddingValues(vertical = 14.dp, horizontal = 4.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "header") {
            Box(
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(tokens.cyanFill)
                    .border(1.5.dp, tokens.inkBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "Lirik teks (tanpa sinkronisasi waktu)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = tokens.onCyanFill,
                )
            }
        }
        itemsIndexed(
            items = lines,
            key = { index, _ -> index },
            contentType = { _, _ -> "plain" },
        ) { _, line ->
            if (line.isEmpty()) {
                Spacer(Modifier.height(14.dp))
            } else {
                Text(
                    text = line,
                    style = LyricLineStyle,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                )
            }
        }
        item(key = "attribution") {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Lirik oleh LRCLIB",
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
            "Sedang offline",
            "Pemutaran tetap berjalan. Sambungkan internet untuk memuat lirik.",
        )
        LyricsError.Timeout -> Triple(
            Icons.Filled.HourglassEmpty,
            "Waktu habis",
            "LRCLIB terlalu lama merespons.",
        )
        is LyricsError.RateLimited -> Triple(
            Icons.Filled.HourglassEmpty,
            "Tunggu sebentar",
            "LRCLIB membatasi permintaan sementara. Coba lagi sekitar ${error.retryAfterSec} detik.",
        )
        is LyricsError.Server -> Triple(
            Icons.Filled.ErrorOutline,
            "Gangguan server",
            "Server LRCLIB sedang bermasalah (HTTP ${error.code}).",
        )
        is LyricsError.Http -> Triple(
            Icons.Filled.ErrorOutline,
            "Permintaan gagal",
            "Gagal memuat lirik (HTTP ${error.code}).",
        )
        LyricsError.Malformed -> Triple(
            Icons.Filled.ErrorOutline,
            "Respons tidak valid",
            "Format respons lirik tidak dapat dibaca.",
        )
        LyricsError.Network -> Triple(
            Icons.Filled.ErrorOutline,
            "Gangguan jaringan",
            "Tidak dapat terhubung ke LRCLIB.",
        )
    }
    Column(modifier = Modifier.fillMaxSize()) {
        CenteredMessage(
            icon = icon,
            title = title,
            body = body,
            actionLabel = "Coba Lagi",
            onAction = onRetry,
        )
    }
}
