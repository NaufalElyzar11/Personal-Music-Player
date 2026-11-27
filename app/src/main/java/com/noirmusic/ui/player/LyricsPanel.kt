package com.noirmusic.ui.player

import android.os.SystemClock
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.noirmusic.data.lyrics.LyricLine
import com.noirmusic.data.lyrics.LyricsError
import com.noirmusic.ui.common.CenteredMessage
import com.noirmusic.ui.common.LoadingView
import com.noirmusic.ui.sakuga.LocalSakugaFx
import com.noirmusic.ui.sakuga.sakugaCelShadow
import com.noirmusic.ui.theme.ActiveLyricLineStyle
import com.noirmusic.ui.theme.LyricLineStyle
import com.noirmusic.ui.theme.SakugaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
            contentPadding = PaddingValues(vertical = verticalPad, horizontal = 6.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(
                items = lines,
                key = { index, _ -> index },
                contentType = { _, _ -> "lyric" },
            ) { index, line ->
                SyncedLyricRow(
                    line = line,
                    isActive = index == activeIndex,
                    onSeekTo = onSeekTo,
                )
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

/**
 * Animated anime lyric row: transitioning into active state triggers a snappy squash-and-stretch pop,
 * horizontal dash-in slide, micro-tilt settle, cel highlight flash, growing accent bar, and subtle beat sparks.
 */
@Composable
private fun SyncedLyricRow(
    line: LyricLine,
    isActive: Boolean,
    onSeekTo: (Long) -> Unit,
) {
    val tokens = SakugaTheme.tokens
    val fx = LocalSakugaFx.current
    val density = LocalDensity.current

    var rowCenter by remember { mutableStateOf(Offset.Zero) }
    var leftEdgeAnchor by remember { mutableStateOf(Offset.Zero) }
    var isFlashing by remember { mutableStateOf(false) }

    // One-shot spring choreography played each time this line becomes active.
    val punchScaleX = remember { Animatable(1f) }
    val punchScaleY = remember { Animatable(1f) }
    val slideOffsetPx = remember { Animatable(0f) }
    val tiltDeg = remember { Animatable(0f) }

    LaunchedEffect(isActive) {
        if (isActive) {
            val startSlidePx = with(density) { (-20).dp.toPx() }
            launch {
                punchScaleX.snapTo(0.92f)
                punchScaleX.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 480f))
            }
            launch {
                punchScaleY.snapTo(1.10f)
                punchScaleY.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 480f))
            }
            launch {
                slideOffsetPx.snapTo(startSlidePx)
                slideOffsetPx.animateTo(0f, spring(dampingRatio = 0.56f, stiffness = 420f))
            }
            launch {
                tiltDeg.snapTo(-1.8f)
                tiltDeg.animateTo(0f, spring(dampingRatio = 0.48f, stiffness = 440f))
            }
            launch {
                isFlashing = true
                delay(110L)
                isFlashing = false
            }
            if (leftEdgeAnchor != Offset.Zero) {
                fx.emitLyricBeat(leftEdgeAnchor)
            }
        } else {
            isFlashing = false
            launch { punchScaleX.animateTo(1f, tween(160)) }
            launch { punchScaleY.animateTo(1f, tween(160)) }
            launch { slideOffsetPx.animateTo(0f, tween(160)) }
            launch { tiltDeg.animateTo(0f, tween(160)) }
        }
    }

    val baseScale by animateFloatAsState(
        targetValue = if (isActive) 1.02f else 0.96f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 380f),
        label = "lyricBaseScale",
    )
    val textColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(220),
        label = "lyricTextColor",
    )
    val containerColor by animateColorAsState(
        targetValue = when {
            !isActive -> Color.Transparent
            isFlashing -> tokens.cyanFill.copy(alpha = 0.24f)
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = tween(160),
        label = "lyricBg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isActive) tokens.inkBorder else Color.Transparent,
        animationSpec = tween(200),
        label = "lyricBorder",
    )
    val shadowColor by animateColorAsState(
        targetValue = if (isActive) tokens.accent else Color.Transparent,
        animationSpec = tween(200),
        label = "lyricShadow",
    )
    val shadowOffset by animateDpAsState(
        targetValue = if (isActive) 4.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.52f, stiffness = 420f),
        label = "lyricShadowOff",
    )
    val accentBarHeight by animateDpAsState(
        targetValue = if (isActive) 28.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.44f, stiffness = 460f),
        label = "lyricBarHeight",
    )
    val verticalPad by animateDpAsState(
        targetValue = if (isActive) 12.dp else 7.dp,
        animationSpec = tween(180),
        label = "lyricVPad",
    )
    val horizontalPad by animateDpAsState(
        targetValue = if (isActive) 14.dp else 8.dp,
        animationSpec = tween(180),
        label = "lyricHPad",
    )

    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (isActive) 6.dp else 4.dp)
            .onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                val w = coords.size.width.toFloat()
                val h = coords.size.height.toFloat()
                rowCenter = Offset(pos.x + w / 2f, pos.y + h / 2f)
                leftEdgeAnchor = Offset(pos.x + with(density) { 10.dp.toPx() }, pos.y + h / 2f)
            }
            .graphicsLayer {
                scaleX = baseScale * punchScaleX.value
                scaleY = baseScale * punchScaleY.value
                translationX = slideOffsetPx.value
                rotationZ = tiltDeg.value
            }
            .sakugaCelShadow(
                color = shadowColor,
                cornerRadius = 14.dp,
                offsetX = shadowOffset,
                offsetY = if (isActive) shadowOffset + 1.dp else 0.dp,
            )
            .clip(shape)
            .background(containerColor, shape)
            .border(if (isActive) 2.5.dp else 0.dp, borderColor, shape)
            .clickable(
                role = Role.Button,
                onClickLabel = "Lompat ke baris lirik",
                onClick = {
                    if (rowCenter != Offset.Zero) {
                        fx.emitSecondarySnap(rowCenter, sfx = "カチッ")
                    }
                    onSeekTo(line.timeMs)
                },
            )
            .padding(horizontal = horizontalPad, vertical = verticalPad),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (accentBarHeight > 1.dp) {
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .height(accentBarHeight)
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
