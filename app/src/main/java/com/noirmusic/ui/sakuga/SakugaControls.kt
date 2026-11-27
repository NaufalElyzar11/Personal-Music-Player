package com.noirmusic.ui.sakuga

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.noirmusic.ui.common.formatDuration
import com.noirmusic.ui.theme.SakugaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

enum class SakugaButtonStyle {
    PRIMARY,
    ACCENT,
    CYAN,
    SECONDARY,
    DANGER,
}

enum class SakugaIconVfx {
    SNAP,
    TOGGLE_SPLASH_ON,
    TOGGLE_SPLASH_OFF,
    PLAY_BLAST,
    SKIP_NEXT,
    SKIP_PREV,
    FAVORITE_ON,
    FAVORITE_OFF,
    THEME_SHIFT,
    PRIMARY_CONFIRM,
    NONE,
}

/** Tracks the root-space center of a composable so Sakuga VFX spawn accurately over it. */
fun Modifier.trackRootCenter(onCenter: (Offset) -> Unit): Modifier =
    onGloballyPositioned { coords ->
        val pos = coords.positionInRoot()
        onCenter(Offset(pos.x + coords.size.width / 2f, pos.y + coords.size.height / 2f))
    }

/** Hard non-blurred cel-shaded offset shadow matching sakuga-vfx (`6px 7px 0 var(--sk-soft)`). */
fun Modifier.sakugaCelShadow(
    color: Color,
    cornerRadius: Dp = 14.dp,
    offsetX: Dp = 4.dp,
    offsetY: Dp = 5.dp,
): Modifier = drawBehind {
    val r = cornerRadius.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(offsetX.toPx(), offsetY.toPx()),
        size = size,
        cornerRadius = CornerRadius(r, r),
    )
}

/** Crisp manga cel panel with thick indigo/violet ink border and optional hard offset shadow. */
@Composable
fun SakugaPanel(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    borderColor: Color = SakugaTheme.tokens.inkBorder,
    shadowColor: Color = SakugaTheme.tokens.celShadow,
    cornerRadius: Dp = 14.dp,
    borderWidth: Dp = 2.5.dp,
    shadowOffsetX: Dp = 4.dp,
    shadowOffsetY: Dp = 5.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .sakugaCelShadow(
                color = shadowColor,
                cornerRadius = cornerRadius,
                offsetX = shadowOffsetX,
                offsetY = shadowOffsetY,
            )
            .clip(shape)
            .background(containerColor, shape)
            .border(borderWidth, borderColor, shape)
            .padding(contentPadding),
        content = content,
    )
}

/**
 * Anime cel button ported from `<sakuga-button>`:
 * press squashes flat and compresses its offset shadow; release flashes for 1 frame,
 * springs back with overshoot, and fires impact bursts, smoke puffs, and manga SFX.
 */
@Composable
fun SakugaButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: SakugaButtonStyle = SakugaButtonStyle.PRIMARY,
    enabled: Boolean = true,
    cornerRadius: Dp = 14.dp,
    sfxText: String? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val tokens = SakugaTheme.tokens
    val fx = LocalSakugaFx.current
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    var center by remember { mutableStateOf(Offset.Zero) }
    var isFlashing by remember { mutableStateOf(false) }

    val baseBg = when (style) {
        SakugaButtonStyle.PRIMARY -> MaterialTheme.colorScheme.primary
        SakugaButtonStyle.ACCENT -> tokens.accent
        SakugaButtonStyle.CYAN -> tokens.cyanFill
        SakugaButtonStyle.SECONDARY -> MaterialTheme.colorScheme.surfaceContainerLow
        SakugaButtonStyle.DANGER -> MaterialTheme.colorScheme.error
    }
    val baseFg = when (style) {
        SakugaButtonStyle.PRIMARY -> MaterialTheme.colorScheme.onPrimary
        SakugaButtonStyle.ACCENT -> tokens.onAccent
        SakugaButtonStyle.CYAN -> tokens.onCyanFill
        SakugaButtonStyle.SECONDARY -> MaterialTheme.colorScheme.onSurface
        SakugaButtonStyle.DANGER -> MaterialTheme.colorScheme.onError
    }

    val containerColor = when {
        !enabled -> MaterialTheme.colorScheme.surfaceVariant
        isFlashing -> tokens.inkBorder
        else -> baseBg
    }
    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
        isFlashing -> tokens.vfxFill
        else -> baseFg
    }

    val scaleX by animateFloatAsState(
        targetValue = if (isPressed && enabled) 1.09f else 1f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = 540f),
        label = "btnScaleX",
    )
    val scaleY by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.86f else 1f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = 540f),
        label = "btnScaleY",
    )
    val pressShift by animateDpAsState(
        targetValue = if (isPressed && enabled) 3.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 620f),
        label = "btnShift",
    )
    val shadowX = (4.dp - pressShift).coerceAtLeast(0.dp)
    val shadowY = (5.dp - pressShift).coerceAtLeast(0.dp)
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .trackRootCenter { center = it }
            .graphicsLayer {
                this.scaleX = scaleX
                this.scaleY = scaleY
                translationX = pressShift.toPx()
                translationY = pressShift.toPx()
            }
            .then(
                if (isFocused) {
                    Modifier
                        .border(3.dp, tokens.accent, RoundedCornerShape(cornerRadius + 2.dp))
                        .padding(2.dp)
                } else {
                    Modifier
                },
            )
            .sakugaCelShadow(
                color = if (enabled) tokens.celShadow else Color.Transparent,
                cornerRadius = cornerRadius,
                offsetX = shadowX,
                offsetY = shadowY,
            )
            .clip(shape)
            .background(containerColor, shape)
            .border(2.5.dp, if (enabled) tokens.inkBorder else MaterialTheme.colorScheme.outlineVariant, shape)
            .focusable(enabled = enabled, interactionSource = interactionSource)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = {
                    if (center != Offset.Zero) {
                        when (style) {
                            SakugaButtonStyle.SECONDARY -> fx.emitSecondarySnap(center, sfx = sfxText ?: "カチッ")
                            else -> fx.emitPrimaryConfirm(center, sfx = sfxText ?: "ドン!")
                        }
                    }
                    scope.launch {
                        isFlashing = true
                        delay(65L)
                        isFlashing = false
                    }
                    onClick()
                },
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides contentColor,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                content = content,
            )
        }
    }
}

/** Tactile 48dp+ anime icon button with squash-and-stretch spring physics and VFX bursts. */
@Composable
fun SakugaIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    vfx: SakugaIconVfx = SakugaIconVfx.SNAP,
    framed: Boolean = false,
    frameColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    frameCornerRadius: Dp = 12.dp,
    contentDescription: String? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val tokens = SakugaTheme.tokens
    val fx = LocalSakugaFx.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    var center by remember { mutableStateOf(Offset.Zero) }

    val scaleX by animateFloatAsState(
        targetValue = if (isPressed && enabled) 1.16f else 1f,
        animationSpec = spring(dampingRatio = 0.38f, stiffness = 560f),
        label = "iconScaleX",
    )
    val scaleY by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.82f else 1f,
        animationSpec = spring(dampingRatio = 0.38f, stiffness = 560f),
        label = "iconScaleY",
    )

    val shape = RoundedCornerShape(frameCornerRadius)

    Box(
        modifier = modifier
            .size(48.dp)
            .trackRootCenter { center = it }
            .graphicsLayer {
                this.scaleX = scaleX
                this.scaleY = scaleY
            }
            .semantics {
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
                this.role = Role.Button
            }
            .then(
                if (isFocused) {
                    Modifier.border(2.5.dp, tokens.accent, shape)
                } else {
                    Modifier
                },
            )
            .focusable(enabled = enabled, interactionSource = interactionSource)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = {
                    if (center != Offset.Zero) {
                        when (vfx) {
                            SakugaIconVfx.SNAP -> fx.emitSecondarySnap(center)
                            SakugaIconVfx.TOGGLE_SPLASH_ON -> fx.emitToggleSplash(center, turnedOn = true)
                            SakugaIconVfx.TOGGLE_SPLASH_OFF -> fx.emitToggleSplash(center, turnedOn = false)
                            SakugaIconVfx.PLAY_BLAST -> fx.emitPlayBlast(center, isNowPlaying = true)
                            SakugaIconVfx.SKIP_NEXT -> fx.emitSkipSlash(center, forward = true)
                            SakugaIconVfx.SKIP_PREV -> fx.emitSkipSlash(center, forward = false)
                            SakugaIconVfx.FAVORITE_ON -> fx.emitFavoriteBurst(center, nowFavorite = true)
                            SakugaIconVfx.FAVORITE_OFF -> fx.emitFavoriteBurst(center, nowFavorite = false)
                            SakugaIconVfx.THEME_SHIFT -> fx.emitThemeShift(center)
                            SakugaIconVfx.PRIMARY_CONFIRM -> fx.emitPrimaryConfirm(center)
                            SakugaIconVfx.NONE -> Unit
                        }
                    }
                    onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (framed) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .sakugaCelShadow(
                        color = if (enabled) tokens.celShadow else Color.Transparent,
                        cornerRadius = frameCornerRadius,
                        offsetX = 2.5.dp,
                        offsetY = 3.dp,
                    )
                    .clip(shape)
                    .background(frameColor, shape)
                    .border(2.dp, if (enabled) tokens.inkBorder else MaterialTheme.colorScheme.outlineVariant, shape),
                contentAlignment = Alignment.Center,
                content = content,
            )
        } else {
            content()
        }
    }
}

/**
 * Animated anime tab bar ported from `<sakuga-tabs>`:
 * the raised cel indicator stretches during travel and lands with wedges, wobbles, and smoke puffs.
 */
@Composable
fun SakugaTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = SakugaTheme.tokens
    val fx = LocalSakugaFx.current
    var previousIndex by remember { mutableIntStateOf(selectedIndex) }
    val indicatorPos = remember { Animatable(selectedIndex.toFloat()) }
    val stretchAnim = remember { Animatable(1f) }
    var rowOrigin by remember { mutableStateOf(Offset.Zero) }
    var rowSize by remember { mutableStateOf(Size.Zero) }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex != previousIndex) {
            val dir = if (selectedIndex > previousIndex) 1f else -1f
            previousIndex = selectedIndex
            launch {
                stretchAnim.snapTo(1.18f)
                stretchAnim.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 460f))
            }
            indicatorPos.animateTo(
                targetValue = selectedIndex.toFloat(),
                animationSpec = spring(dampingRatio = 0.62f, stiffness = 420f),
            )
            if (rowSize.width > 0f && tabs.isNotEmpty()) {
                val tabW = rowSize.width / tabs.size
                val targetCenter = Offset(
                    x = rowOrigin.x + tabW * (selectedIndex + 0.5f),
                    y = rowOrigin.y + rowSize.height / 2f,
                )
                fx.emitTabLand(targetCenter, dir)
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .height(48.dp)
            .sakugaCelShadow(tokens.celShadow, cornerRadius = 14.dp, offsetX = 3.dp, offsetY = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(2.5.dp, tokens.inkBorder, RoundedCornerShape(14.dp))
            .padding(4.dp)
            .onGloballyPositioned { coords ->
                rowOrigin = coords.positionInRoot()
                rowSize = Size(coords.size.width.toFloat(), coords.size.height.toFloat())
            },
    ) {
        val count = tabs.size.coerceAtLeast(1)
        val slotWidth = maxWidth / count
        val density = LocalDensity.current

        // Sliding cel pill with velocity smear stretch
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = with(density) { (slotWidth * indicatorPos.value).toPx().roundToInt() },
                        y = 0,
                    )
                }
                .width(slotWidth)
                .fillMaxHeight()
                .graphicsLayer {
                    scaleX = stretchAnim.value
                    scaleY = 2f - stretchAnim.value
                }
                .clip(RoundedCornerShape(10.dp))
                .background(tokens.accent)
                .border(2.dp, tokens.inkBorder, RoundedCornerShape(10.dp)),
        )

        Row(modifier = Modifier.fillMaxSize()) {
            tabs.forEachIndexed { idx, label ->
                val isSelected = idx == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics {
                            selected = isSelected
                            role = Role.Tab
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(idx) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        fontStyle = if (isSelected) FontStyle.Italic else FontStyle.Normal,
                        color = if (isSelected) tokens.onAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Two-option anime smear switch (Cover/Lirik, Baru Diputar/Sering Diputar) ported from `<sakuga-toggle>`.
 */
@Composable
fun SakugaSegmentedSwitch(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = SakugaTheme.tokens
    val fx = LocalSakugaFx.current
    var prevIndex by remember { mutableIntStateOf(selectedIndex) }
    val slide = remember { Animatable(selectedIndex.toFloat()) }
    val smear = remember { Animatable(1f) }
    var boxOrigin by remember { mutableStateOf(Offset.Zero) }
    var boxSize by remember { mutableStateOf(Size.Zero) }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex != prevIndex) {
            val turnedRight = selectedIndex > prevIndex
            prevIndex = selectedIndex
            launch {
                smear.snapTo(1.22f)
                smear.animateTo(1f, spring(dampingRatio = 0.38f, stiffness = 500f))
            }
            slide.animateTo(selectedIndex.toFloat(), spring(dampingRatio = 0.58f, stiffness = 460f))
            if (boxSize.width > 0f && options.isNotEmpty()) {
                val slotW = boxSize.width / options.size
                fx.emitToggleSplash(
                    center = Offset(boxOrigin.x + slotW * (selectedIndex + 0.5f), boxOrigin.y + boxSize.height / 2f),
                    turnedOn = turnedRight,
                    baselineDropDp = 22f,
                )
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .height(44.dp)
            .sakugaCelShadow(tokens.celShadow, cornerRadius = 12.dp, offsetX = 3.dp, offsetY = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(2.5.dp, tokens.inkBorder, RoundedCornerShape(12.dp))
            .padding(3.dp)
            .onGloballyPositioned { coords ->
                boxOrigin = coords.positionInRoot()
                boxSize = Size(coords.size.width.toFloat(), coords.size.height.toFloat())
            },
    ) {
        val count = options.size.coerceAtLeast(1)
        val slotW = maxWidth / count
        val density = LocalDensity.current

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(with(density) { (slotW * slide.value).toPx().roundToInt() }, 0)
                }
                .width(slotW)
                .fillMaxHeight()
                .graphicsLayer {
                    scaleX = smear.value
                    scaleY = 1f / smear.value.coerceAtLeast(0.8f)
                }
                .clip(RoundedCornerShape(9.dp))
                .background(tokens.cyanFill)
                .border(2.dp, tokens.inkBorder, RoundedCornerShape(9.dp)),
        )

        Row(modifier = Modifier.fillMaxSize()) {
            options.forEachIndexed { idx, label ->
                val isSelected = idx == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics {
                            selected = isSelected
                            role = Role.Tab
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(idx) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        color = if (isSelected) tokens.onCyanFill else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Custom audio seek scrubber ported from `<sakuga-slider>`:
 * teardrop thumb stretches with velocity on a spring, kicks up rolling cumulus clouds during fast drag,
 * pops a comic speech-bubble timecode tooltip, and fires a crescent slash on release.
 */
@Composable
fun SakugaSeekSlider(
    valueMs: Long,
    durationMs: Long,
    onSeekCommit: (Long) -> Unit,
    onDragStateChange: (isDragging: Boolean, dragMs: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = SakugaTheme.tokens
    val fx = LocalSakugaFx.current
    val enabled = durationMs > 0L
    val density = LocalDensity.current

    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var dragVelocity by remember { mutableFloatStateOf(0f) }
    var lastSmokeMs by remember { mutableLongStateOf(0L) }
    var barOrigin by remember { mutableStateOf(Offset.Zero) }
    var barWidthPx by remember { mutableFloatStateOf(1f) }
    var barHeightPx by remember { mutableFloatStateOf(1f) }

    val playbackFraction = if (enabled) (valueMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    val targetFraction = if (isDragging) dragFraction else playbackFraction

    // Spring-lagged visual thumb position and velocity stretch
    val visualFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = if (isDragging) {
            spring(dampingRatio = 0.65f, stiffness = 750f)
        } else {
            tween(durationMillis = 140, easing = LinearEasing)
        },
        label = "seekThumb",
    )
    val stretch by animateFloatAsState(
        targetValue = if (isDragging) (dragVelocity / 45f).coerceIn(-0.42f, 0.42f) else 0f,
        animationSpec = spring(dampingRatio = 0.48f, stiffness = 420f),
        label = "thumbStretch",
    )

    val displayedMs = (targetFraction * durationMs.coerceAtLeast(0L)).toLong()
    val elapsedText = formatDuration(displayedMs)
    val remainingText = if (enabled) "-${formatDuration((durationMs - displayedMs).coerceAtLeast(0L))}" else "--:--"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .onGloballyPositioned { coords ->
                barOrigin = coords.positionInRoot()
                barWidthPx = coords.size.width.toFloat().coerceAtLeast(1f)
                barHeightPx = coords.size.height.toFloat().coerceAtLeast(1f)
            }
            .semantics {
                contentDescription = "Seek"
                stateDescription = "$elapsedText dari $remainingText"
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = targetFraction,
                    range = 0f..1f,
                )
            }
            .pointerInput(enabled, durationMs) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    val pad = 16.dp.toPx()
                    val usable = (size.width - pad * 2f).coerceAtLeast(1f)
                    val frac = ((offset.x - pad) / usable).coerceIn(0f, 1f)
                    val targetMs = (frac * durationMs).toLong()
                    onSeekCommit(targetMs)
                    val dirSign = if (frac >= playbackFraction) 1f else -1f
                    fx.emitScrubRelease(
                        pos = Offset(barOrigin.x + pad + usable * frac, barOrigin.y + barHeightPx / 2f),
                        dirSign = dirSign,
                    )
                }
            }
            .pointerInput(enabled, durationMs) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { startOffset ->
                        val pad = 16.dp.toPx()
                        val usable = (size.width - pad * 2f).coerceAtLeast(1f)
                        isDragging = true
                        dragFraction = ((startOffset.x - pad) / usable).coerceIn(0f, 1f)
                        dragVelocity = 0f
                        onDragStateChange(true, (dragFraction * durationMs).toLong())
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val pad = 16.dp.toPx()
                        val usable = (size.width - pad * 2f).coerceAtLeast(1f)
                        dragFraction = (dragFraction + dragAmount / usable).coerceIn(0f, 1f)
                        dragVelocity = dragAmount
                        onDragStateChange(true, (dragFraction * durationMs).toLong())

                        val now = android.os.SystemClock.elapsedRealtime()
                        if (abs(dragAmount) > 5f && now - lastSmokeMs > 65L) {
                            lastSmokeMs = now
                            fx.emitScrubSmoke(
                                pos = Offset(barOrigin.x + pad + usable * dragFraction, barOrigin.y + barHeightPx / 2f),
                                velocityX = dragAmount * 14f,
                            )
                        }
                    },
                    onDragEnd = {
                        val commitMs = (dragFraction * durationMs).toLong()
                        val dirSign = if (dragVelocity >= 0f) 1f else -1f
                        val pad = with(density) { 16.dp.toPx() }
                        val usable = (barWidthPx - pad * 2f).coerceAtLeast(1f)
                        isDragging = false
                        dragVelocity = 0f
                        onDragStateChange(false, commitMs)
                        onSeekCommit(commitMs)
                        fx.emitScrubRelease(
                            pos = Offset(barOrigin.x + pad + usable * dragFraction, barOrigin.y + barHeightPx / 2f),
                            dirSign = dirSign,
                        )
                    },
                    onDragCancel = {
                        isDragging = false
                        dragVelocity = 0f
                        onDragStateChange(false, valueMs)
                    },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        // Floating manga speech bubble tooltip while scrubbing
        AnimatedVisibility(
            visible = isDragging && enabled,
            enter = scaleIn(spring(dampingRatio = 0.45f, stiffness = 520f)) + fadeIn(tween(80)),
            exit = scaleOut(tween(110)) + fadeOut(tween(110)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset {
                    val pad = with(density) { 16.dp.toPx() }
                    val usable = (barWidthPx - pad * 2f).coerceAtLeast(1f)
                    val bubbleHalf = with(density) { 28.dp.toPx() }
                    val x = (pad + usable * visualFraction - bubbleHalf).coerceIn(0f, (barWidthPx - bubbleHalf * 2f).coerceAtLeast(0f))
                    IntOffset(x.roundToInt(), with(density) { (-28).dp.toPx().roundToInt() })
                },
        ) {
            Box(
                modifier = Modifier
                    .sakugaCelShadow(tokens.celShadow, cornerRadius = 8.dp, offsetX = 2.dp, offsetY = 3.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(tokens.inkBorder)
                    .border(2.dp, tokens.accent, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    text = elapsedText,
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.vfxFill,
                    fontWeight = FontWeight.Black,
                )
            }
        }

        val trackBg = MaterialTheme.colorScheme.surfaceVariant
        val activeFill = tokens.accent
        val stripeColor = tokens.vfxFill.copy(alpha = 0.28f)
        val inkColor = tokens.inkBorder
        val shadowColor = tokens.celShadow
        val thumbCore = if (isDragging) tokens.cyanFill else tokens.vfxFill

        Canvas(modifier = Modifier.fillMaxSize()) {
            val pad = 16.dp.toPx()
            val trackH = 12.dp.toPx()
            val usable = (size.width - pad * 2f).coerceAtLeast(1f)
            val cy = size.height / 2f
            val r = trackH / 2f

            // Cel shadow under track
            drawRoundRect(
                color = shadowColor,
                topLeft = Offset(pad + 2.dp.toPx(), cy - r + 3.dp.toPx()),
                size = Size(usable, trackH),
                cornerRadius = CornerRadius(r, r),
            )

            // Inactive track base
            val trackRect = Rect(pad, cy - r, pad + usable, cy + r)
            val trackPath = Path().apply { addRoundRect(RoundRect(trackRect, CornerRadius(r, r))) }
            drawPath(trackPath, color = trackBg)

            // Filled active track with diagonal manga speed stripes
            val fillW = (usable * visualFraction).coerceIn(0f, usable)
            if (fillW > 2f) {
                clipPath(trackPath) {
                    drawRect(
                        color = activeFill,
                        topLeft = Offset(pad, cy - r),
                        size = Size(fillW, trackH),
                    )
                    val step = 12.dp.toPx()
                    var sx = pad - trackH
                    while (sx < pad + fillW + trackH) {
                        drawLine(
                            color = stripeColor,
                            start = Offset(sx, cy + r),
                            end = Offset(sx + trackH * 0.9f, cy - r),
                            strokeWidth = 3.dp.toPx(),
                        )
                        sx += step
                    }
                }
            }

            // Crisp manga ink border around track
            drawPath(
                path = trackPath,
                color = inkColor,
                style = Stroke(width = 2.2.dp.toPx(), join = StrokeJoin.Round),
            )

            // Teardrop stretching cel thumb
            val thumbX = pad + fillW
            val baseR = (if (isDragging) 13.5.dp else 11.dp).toPx()
            val tailFactor = stretch * 1.45f
            val thumbPath = Path().apply {
                val leftExtent = baseR * (1f + (-tailFactor).coerceAtLeast(0f) * 1.4f)
                val rightExtent = baseR * (1f + tailFactor.coerceAtLeast(0f) * 1.4f)
                val verticalR = baseR * (1f - abs(stretch) * 0.28f)
                moveTo(thumbX - leftExtent, cy)
                quadraticTo(thumbX - baseR * 0.55f, cy - verticalR * 1.15f, thumbX, cy - verticalR)
                quadraticTo(thumbX + baseR * 0.55f, cy - verticalR * 1.15f, thumbX + rightExtent, cy)
                quadraticTo(thumbX + baseR * 0.55f, cy + verticalR * 1.15f, thumbX, cy + verticalR)
                quadraticTo(thumbX - baseR * 0.55f, cy + verticalR * 1.15f, thumbX - leftExtent, cy)
                close()
            }

            // Thumb cel shadow
            translate(left = 2.5.dp.toPx(), top = 3.dp.toPx()) {
                drawPath(
                    path = thumbPath,
                    color = shadowColor,
                )
            }
            drawPath(thumbPath, color = thumbCore)
            drawPath(
                path = thumbPath,
                color = inkColor,
                style = Stroke(width = 2.6.dp.toPx(), join = StrokeJoin.Round),
            )
            // Inner sakura/ink dot inside thumb
            drawCircle(
                color = if (isDragging) inkColor else tokens.accent,
                radius = 3.5.dp.toPx(),
                center = Offset(thumbX, cy),
            )
        }
    }
}

/** Anime cel search bar ported from `<sakuga-input>`: every keystroke kicks up dust puffs & glints. */
@Composable
fun SakugaSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchAction: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Cari lagu, artis, atau album",
) {
    val tokens = SakugaTheme.tokens
    val fx = LocalSakugaFx.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    var origin by remember { mutableStateOf(Offset.Zero) }
    var sizePx by remember { mutableStateOf(Size.Zero) }

    val borderColor = if (isFocused) tokens.accent else tokens.inkBorder
    val shape = RoundedCornerShape(14.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .onGloballyPositioned { coords ->
                origin = coords.positionInRoot()
                sizePx = Size(coords.size.width.toFloat(), coords.size.height.toFloat())
            }
            .sakugaCelShadow(
                color = if (isFocused) tokens.accent.copy(alpha = 0.45f) else tokens.celShadow,
                cornerRadius = 14.dp,
                offsetX = 3.dp,
                offsetY = 4.dp,
            )
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow, shape)
            .border(2.5.dp, borderColor, shape)
            .padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = if (isFocused) tokens.accentText else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (query.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = { next ->
                    if (next != query && sizePx.width > 0f) {
                        val approxX = (origin.x + 56f + next.length * 16f)
                            .coerceIn(origin.x + 48f, origin.x + sizePx.width - 56f)
                        fx.emitKeystrokeDust(Offset(approxX, origin.y + sizePx.height * 0.55f))
                    }
                    onQueryChange(next)
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(tokens.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearchAction() }),
                interactionSource = interactionSource,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            SakugaIconButton(
                onClick = { onQueryChange("") },
                vfx = SakugaIconVfx.SNAP,
                contentDescription = "Hapus pencarian",
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** Colorful 4-bar anime cel spectrum that dances ONLY while audio is playing (calm when paused). */
@Composable
fun SakugaEqualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val tokens = SakugaTheme.tokens
    val barColors = listOf(tokens.accent, tokens.cyanFill, MaterialTheme.colorScheme.primary, tokens.accent)

    if (!isPlaying) {
        Row(
            modifier = modifier.size(22.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            listOf(6.dp, 11.dp, 8.dp, 5.dp).forEachIndexed { i, h ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(h)
                        .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                        .background(barColors[i])
                        .border(1.dp, tokens.inkBorder, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)),
                )
            }
        }
        return
    }

    val infinite = rememberInfiniteTransition(label = "sakugaEq")
    val h1 by infinite.animateFloat(
        initialValue = 5f,
        targetValue = 19f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq1",
    )
    val h2 by infinite.animateFloat(
        initialValue = 18f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(340, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq2",
    )
    val h3 by infinite.animateFloat(
        initialValue = 8f,
        targetValue = 21f,
        animationSpec = infiniteRepeatable(tween(490, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq3",
    )
    val h4 by infinite.animateFloat(
        initialValue = 15f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(380, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq4",
    )
    val heights = listOf(h1, h2, h3, h4)

    Row(
        modifier = modifier.size(22.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        heights.forEachIndexed { i, h ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(h.dp)
                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    .background(barColors[i])
                    .border(1.dp, tokens.inkBorder, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)),
            )
        }
    }
}

/**
 * Manga halftone dots + radial action speed-lines behind the Now Playing stage.
 * Rotates gently ONLY while audio is playing; rests still when paused.
 */
@Composable
fun SakugaStageBackdrop(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val tokens = SakugaTheme.tokens
    val rotationDeg = if (isPlaying) {
        val infinite = rememberInfiniteTransition(label = "stageSpin")
        val deg by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(26_000, easing = LinearEasing)),
            label = "rays",
        )
        deg
    } else {
        0f
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width * 0.5f
        val cy = size.height * 0.36f
        val maxR = size.maxDimension * 0.92f

        // Radial comic focus lines framing the artwork center
        rotate(degrees = rotationDeg, pivot = Offset(cx, cy)) {
            val rayCount = 28
            for (i in 0 until rayCount) {
                val angle = (i.toFloat() / rayCount) * (2.0 * PI).toFloat()
                val innerR = size.minDimension * if (i % 3 == 0) 0.42f else 0.52f
                val spread = if (i % 3 == 0) 0.026f else 0.014f
                val ray = Path().apply {
                    moveTo(cx + cos(angle) * innerR, cy + sin(angle) * innerR)
                    lineTo(cx + cos(angle - spread) * maxR, cy + sin(angle - spread) * maxR)
                    lineTo(cx + cos(angle + spread) * maxR, cy + sin(angle + spread) * maxR)
                    close()
                }
                drawPath(ray, color = tokens.speedLine)
            }
        }

        // Comic manga screentone halftone dots along top-left & bottom-right corners
        val dotStep = 14.dp.toPx()
        val maxDotR = 2.6.dp.toPx()
        var y = dotStep * 0.5f
        while (y < size.height * 0.24f) {
            val strength = 1f - (y / (size.height * 0.24f))
            var x = dotStep * 0.5f
            while (x < size.width) {
                val r = maxDotR * strength
                if (r > 0.7f) {
                    drawCircle(color = tokens.halftoneDot, radius = r, center = Offset(x, y))
                }
                x += dotStep
            }
            y += dotStep
        }
    }
}

/** Anime toast host inspired by `<sakuga-toast>`: streaks in with cel framing and fires ズザッ skid VFX. */
@Composable
fun SakugaSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val tokens = SakugaTheme.tokens
    val fx = LocalSakugaFx.current
    var center by remember { mutableStateOf(Offset.Zero) }

    SnackbarHost(
        hostState = hostState,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 10.dp),
    ) { data ->
        LaunchedEffect(data) {
            delay(50L)
            if (center != Offset.Zero) {
                fx.emitSlideSkid(center)
            }
        }

        AnimatedVisibility(
            visible = true,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = spring(dampingRatio = 0.58f, stiffness = 460f),
            ) + fadeIn(tween(120)),
            exit = slideOutHorizontally(targetOffsetX = { -it / 2 }) + fadeOut(tween(140)),
        ) {
            SakugaPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .trackRootCenter { center = it },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                borderColor = tokens.inkBorder,
                shadowColor = tokens.accent.copy(alpha = 0.75f),
                cornerRadius = 12.dp,
                shadowOffsetX = 4.dp,
                shadowOffsetY = 4.dp,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(tokens.accent)
                            .border(1.5.dp, tokens.inkBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "SFX",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            color = tokens.onAccent,
                        )
                    }
                    Text(
                        text = data.visuals.message,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** Deterministic vibrant 3-color track accent resolver strictly within our Sakuga 2-core + 1-accent palette. */
@Composable
fun sakugaTrackTint(trackId: Long): Pair<Color, Color> {
    val tokens = SakugaTheme.tokens
    return when ((abs(trackId) % 3L).toInt()) {
        0 -> tokens.violetFill to tokens.onVioletFill
        1 -> tokens.cyanFill to tokens.onCyanFill
        else -> tokens.accent to tokens.onAccent
    }
}

/**
 * Centered full-viewport Sakuga dialog host with its own local VFX overlay canvas so anime bursts
 * render visibly above the dialog while tapping outside dismisses cleanly.
 */
@Composable
fun SakugaDialog(
    onDismissRequest: () -> Unit,
    horizontalPadding: Dp = 18.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        SakugaFxHost(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismissRequest,
                    )
                    .padding(horizontal = horizontalPadding),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        ),
                    contentAlignment = Alignment.Center,
                    content = content,
                )
            }
        }
    }
}
