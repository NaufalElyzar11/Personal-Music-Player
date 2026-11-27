package com.noirmusic.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.noirmusic.playback.SleepTimerState
import com.noirmusic.ui.common.formatDuration
import com.noirmusic.ui.sakuga.LocalSakugaFx
import com.noirmusic.ui.sakuga.SakugaIconButton
import com.noirmusic.ui.sakuga.SakugaIconVfx
import com.noirmusic.ui.sakuga.SakugaSeekSlider
import com.noirmusic.ui.sakuga.SakugaSegmentedSwitch
import com.noirmusic.ui.sakuga.SakugaStageBackdrop
import com.noirmusic.ui.sakuga.sakugaCelShadow
import com.noirmusic.ui.sakuga.trackRootCenter
import com.noirmusic.ui.theme.SakugaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

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
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onAddToPlaylist: (() -> Unit)? = null,
    onSkipToQueueItem: (Int) -> Unit = {},
    onMoveQueueItem: (Int, Int) -> Unit = { _, _ -> },
    onRemoveQueueItem: (Int) -> Unit = {},
    onClearUpcomingQueue: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val tokens = SakugaTheme.tokens
    val fx = LocalSakugaFx.current
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
            onMoveQueueItem = onMoveQueueItem,
            onRemoveQueueItem = onRemoveQueueItem,
            onClearUpcomingQueue = onClearUpcomingQueue,
            onDismiss = { showQueueDialog = false },
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Dynamic anime speed-lines + halftone comic backdrop (moves ONLY while playing)
        SakugaStageBackdrop(isPlaying = playback.isPlaying)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            // 1. Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SakugaIconButton(
                    onClick = onClose,
                    vfx = SakugaIconVfx.SKIP_PREV,
                    framed = true,
                    contentDescription = "Tutup pemutar musik",
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(26.dp),
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = if (playback.isPlaying) "ON AIR · LIBRARY" else "SIAP DIPUTAR",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        color = tokens.accentText,
                    )
                    Text(
                        text = track?.album ?: "Noir Music",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (playback.queueSize > 0) {
                        SakugaIconButton(
                            onClick = { showQueueDialog = true },
                            vfx = SakugaIconVfx.SNAP,
                            framed = true,
                            contentDescription = "Buka antrean",
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    if (onAddToPlaylist != null && track != null) {
                        SakugaIconButton(
                            onClick = onAddToPlaylist,
                            vfx = SakugaIconVfx.SNAP,
                            framed = true,
                            contentDescription = "Tambah ke playlist",
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            // 2. Cover / Lyrics Sakuga Smear Switch
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                SakugaSegmentedSwitch(
                    options = listOf("Cover", "Lirik"),
                    selectedIndex = pane.ordinal,
                    onSelect = { idx ->
                        onPaneChange(if (idx == 0) NowPlayingPane.COVER else NowPlayingPane.LYRICS)
                    },
                    modifier = Modifier.widthIn(min = 210.dp, max = 230.dp),
                )
            }

            // 3. Central Stage (Hero Manga Cover / Animated Synced Lyrics)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 6.dp),
            ) {
                AnimatedContent(
                    targetState = pane,
                    transitionSpec = {
                        val dir = if (targetState == NowPlayingPane.LYRICS) 1 else -1
                        (slideInHorizontally(spring(dampingRatio = 0.72f, stiffness = 420f)) { it / 2 * dir } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it / 3 * dir } + fadeOut())
                    },
                    label = "paneSwitch",
                ) { activePane ->
                    if (activePane == NowPlayingPane.COVER) {
                        CoverPane(
                            artworkFlow = artworkFlow,
                            isPlaying = playback.isPlaying,
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
            }

            // 4. Title & Artist + Mega Sakuga Favorite Heart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 16.dp, top = 4.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track?.title ?: "Tidak ada lagu",
                        style = MaterialTheme.typography.headlineMedium,
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
                if (track != null) {
                    SakugaIconButton(
                        onClick = onToggleFavorite,
                        vfx = if (isFavorite) SakugaIconVfx.FAVORITE_OFF else SakugaIconVfx.FAVORITE_ON,
                        framed = true,
                        frameColor = if (isFavorite) tokens.accent else MaterialTheme.colorScheme.surfaceContainerLow,
                        contentDescription = if (isFavorite) "Hapus dari favorit" else "Tambah ke favorit",
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFavorite) tokens.onAccent else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }

            // 5. Sakuga Seek Slider (Teardrop stretching thumb + cumulus clouds + release slash)
            SeekSection(
                positionMsFlow = positionMsFlow,
                durationMs = playback.durationMs,
                onSeekTo = onSeekTo,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            )

            if (playback.errorMessage != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "File tidak dapat diputar · ${playback.errorMessage}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }

            Spacer(Modifier.height(6.dp))

            // 6. Hero Sakuga Transport Controls
            ControlsRow(
                playback = playback,
                onTogglePlayPause = onTogglePlayPause,
                onPrevious = onPrevious,
                onNext = onNext,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeat = onCycleRepeat,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
            )

            Spacer(Modifier.height(10.dp))

            // 7. Bottom Auxiliary Bar (Sleep Timer & Queue Chip)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
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

                var timerCenter by remember { mutableStateOf(Offset.Zero) }
                Row(
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .trackRootCenter { timerCenter = it }
                        .sakugaCelShadow(
                            color = if (isTimerActive) tokens.accent else tokens.celShadow,
                            cornerRadius = 10.dp,
                            offsetX = 2.5.dp,
                            offsetY = 3.dp,
                        )
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isTimerActive) tokens.cyanFill else MaterialTheme.colorScheme.surfaceContainerLow)
                        .border(2.dp, tokens.inkBorder, RoundedCornerShape(10.dp))
                        .clickable {
                            if (timerCenter != Offset.Zero) fx.emitSecondarySnap(timerCenter)
                            showSleepTimerDialog = true
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bedtime,
                        contentDescription = "Timer tidur",
                        tint = if (isTimerActive) tokens.onCyanFill else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = timerText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = if (isTimerActive) tokens.onCyanFill else MaterialTheme.colorScheme.onSurface,
                    )
                }

                if (playback.queueSize > 0) {
                    var queueCenter by remember { mutableStateOf(Offset.Zero) }
                    Row(
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .trackRootCenter { queueCenter = it }
                            .sakugaCelShadow(tokens.celShadow, cornerRadius = 10.dp, offsetX = 2.5.dp, offsetY = 3.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .border(2.dp, tokens.inkBorder, RoundedCornerShape(10.dp))
                            .clickable {
                                if (queueCenter != Offset.Zero) fx.emitSecondarySnap(queueCenter)
                                showQueueDialog = true
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Buka antrean",
                            tint = tokens.accentText,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "${playback.queueIndex + 1} / ${playback.queueSize}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
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
    var dragMs by remember { mutableLongStateOf(0L) }

    val shownMs = if (isDragging) {
        dragMs
    } else {
        positionMs.coerceIn(0L, durationMs.coerceAtLeast(0L))
    }
    val elapsedText = formatDuration(shownMs)
    val remainingMs = (durationMs - shownMs).coerceAtLeast(0L)
    val remainingText = if (enabled) "-${formatDuration(remainingMs)}" else "--:--"

    Column(modifier = modifier) {
        SakugaSeekSlider(
            valueMs = shownMs,
            durationMs = durationMs,
            onSeekCommit = onSeekTo,
            onDragStateChange = { dragging, ms ->
                isDragging = dragging
                dragMs = ms
            },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = elapsedText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
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
    val tokens = SakugaTheme.tokens
    val fx = LocalSakugaFx.current
    val scope = rememberCoroutineScope()
    val repeatDescription = when (playback.repeatMode) {
        Player.REPEAT_MODE_ALL -> "Repeat all"
        Player.REPEAT_MODE_ONE -> "Repeat one"
        else -> "Repeat off"
    }

    val playInteraction = remember { MutableInteractionSource() }
    val playPressed by playInteraction.collectIsPressedAsState()
    var playCenter by remember { mutableStateOf(Offset.Zero) }
    var playFlashing by remember { mutableStateOf(false) }

    val playScaleX by animateFloatAsState(
        targetValue = if (playPressed) 1.16f else 1f,
        animationSpec = spring(dampingRatio = 0.36f, stiffness = 540f),
        label = "playSX",
    )
    val playScaleY by animateFloatAsState(
        targetValue = if (playPressed) 0.82f else 1f,
        animationSpec = spring(dampingRatio = 0.36f, stiffness = 540f),
        label = "playSY",
    )
    val playShift by animateDpAsState(
        targetValue = if (playPressed) 4.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f),
        label = "playShift",
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Shuffle toggle
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(52.dp),
        ) {
            SakugaIconButton(
                onClick = onToggleShuffle,
                vfx = if (playback.shuffleEnabled) {
                    SakugaIconVfx.TOGGLE_SPLASH_OFF
                } else {
                    SakugaIconVfx.TOGGLE_SPLASH_ON
                },
                framed = true,
                frameColor = if (playback.shuffleEnabled) tokens.cyanFill else MaterialTheme.colorScheme.surfaceContainerLow,
                contentDescription = if (playback.shuffleEnabled) "Shuffle on" else "Shuffle off",
            ) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = null,
                    tint = if (playback.shuffleEnabled) tokens.onCyanFill else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (playback.shuffleEnabled) tokens.accent else Color.Transparent),
            )
        }

        // Previous track with directional slash VFX
        SakugaIconButton(
            onClick = onPrevious,
            enabled = playback.hasPrevious,
            vfx = SakugaIconVfx.SKIP_PREV,
            framed = true,
            frameColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentDescription = "Previous track",
            modifier = Modifier.size(52.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = null,
                tint = if (playback.hasPrevious) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp),
            )
        }

        // Mega Hero Sakuga Play / Pause Trigger
        val playBg = when {
            playFlashing -> tokens.inkBorder
            playback.isPlaying -> tokens.accent
            else -> tokens.violetFill
        }
        val playFg = when {
            playFlashing -> tokens.vfxFill
            playback.isPlaying -> tokens.onAccent
            else -> tokens.onVioletFill
        }

        Box(
            modifier = Modifier
                .size(76.dp)
                .trackRootCenter { playCenter = it }
                .graphicsLayer {
                    scaleX = playScaleX
                    scaleY = playScaleY
                    translationX = playShift.toPx()
                    translationY = playShift.toPx()
                }
                .sakugaCelShadow(
                    color = if (playback.isPlaying) tokens.cyanFill else tokens.celShadow,
                    cornerRadius = 38.dp,
                    offsetX = (4.dp - playShift).coerceAtLeast(0.dp),
                    offsetY = (5.dp - playShift).coerceAtLeast(0.dp),
                )
                .clip(CircleShape)
                .background(playBg)
                .border(3.dp, tokens.inkBorder, CircleShape)
                .clickable(
                    enabled = playback.queueSize > 0,
                    interactionSource = playInteraction,
                    indication = null,
                    role = Role.Button,
                    onClick = {
                        if (playCenter != Offset.Zero) {
                            fx.emitPlayBlast(playCenter, isNowPlaying = !playback.isPlaying)
                        }
                        scope.launch {
                            playFlashing = true
                            delay(65L)
                            playFlashing = false
                        }
                        onTogglePlayPause()
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (playback.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (playback.isPlaying) "Pause" else "Play",
                tint = playFg,
                modifier = Modifier.size(36.dp),
            )
            if (playback.isBuffering) {
                CircularProgressIndicator(
                    color = tokens.inkBorder,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(66.dp),
                )
            }
        }

        // Next track with directional slash VFX
        SakugaIconButton(
            onClick = onNext,
            enabled = playback.hasNext || playback.repeatMode != Player.REPEAT_MODE_OFF,
            vfx = SakugaIconVfx.SKIP_NEXT,
            framed = true,
            frameColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentDescription = "Next track",
            modifier = Modifier.size(52.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = null,
                tint = if (playback.hasNext || playback.repeatMode != Player.REPEAT_MODE_OFF) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(26.dp),
            )
        }

        // Repeat cycle toggle
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(52.dp),
        ) {
            val repeatActive = playback.repeatMode != Player.REPEAT_MODE_OFF
            SakugaIconButton(
                onClick = onCycleRepeat,
                vfx = if (repeatActive) {
                    SakugaIconVfx.TOGGLE_SPLASH_OFF
                } else {
                    SakugaIconVfx.TOGGLE_SPLASH_ON
                },
                framed = true,
                frameColor = if (repeatActive) tokens.cyanFill else MaterialTheme.colorScheme.surfaceContainerLow,
                contentDescription = repeatDescription,
            ) {
                Icon(
                    imageVector = if (playback.repeatMode == Player.REPEAT_MODE_ONE) {
                        Icons.Filled.RepeatOne
                    } else {
                        Icons.Filled.Repeat
                    },
                    contentDescription = null,
                    tint = if (repeatActive) tokens.onCyanFill else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (repeatActive) tokens.accent else Color.Transparent),
            )
        }
    }
}

@Composable
private fun CoverPane(
    artworkFlow: StateFlow<androidx.compose.ui.graphics.ImageBitmap?>,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val artwork by artworkFlow.collectAsStateWithLifecycle()
    Artwork(bitmap = artwork, isPlaying = isPlaying, modifier = modifier)
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
