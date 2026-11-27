package com.noirmusic.ui.library

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noirmusic.data.media.TrackListLogic
import com.noirmusic.ui.sakuga.LocalSakugaFx
import com.noirmusic.ui.sakuga.sakugaCelShadow
import com.noirmusic.ui.theme.SakugaTheme
import kotlin.math.roundToInt

/**
 * Right-edge Sakuga cel fast-scroller rail (0-9, A-Z) with a bouncy comic speech-bubble callout
 * and subtle wedge/puff particles on symbol jumps.
 */
@Composable
fun AlphabetFastScroller(
    availableSymbols: Set<Char>,
    currentSymbol: Char?,
    onSymbolSelected: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = SakugaTheme.tokens
    val fx = LocalSakugaFx.current
    val symbols = TrackListLogic.FAST_SCROLL_SYMBOLS
    var railHeightPx by remember { mutableFloatStateOf(0f) }
    var railOrigin by remember { mutableStateOf(Offset.Zero) }
    var activeDragSymbol by remember { mutableStateOf<Char?>(null) }
    var dragYOffsetPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val popScale = remember { Animatable(1f) }

    LaunchedEffect(activeDragSymbol) {
        if (activeDragSymbol != null) {
            popScale.snapTo(1.28f)
            popScale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 520f))
        }
    }

    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.CenterEnd,
    ) {
        val previewChar = activeDragSymbol
        if (previewChar != null && railHeightPx > 0f) {
            val badgeSizePx = with(density) { 56.dp.toPx() }
            val clampedY = (dragYOffsetPx - badgeSizePx / 2f)
                .coerceIn(0f, (railHeightPx - badgeSizePx).coerceAtLeast(0f))

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 38.dp)
                    .offset { IntOffset(0, clampedY.roundToInt()) }
                    .size(56.dp)
                    .graphicsLayer {
                        scaleX = popScale.value
                        scaleY = popScale.value
                        rotationZ = -6f
                    }
                    .sakugaCelShadow(tokens.accent, cornerRadius = 14.dp, offsetX = 4.dp, offsetY = 5.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tokens.cyanFill)
                    .border(2.5.dp, tokens.inkBorder, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = previewChar.toString(),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    color = tokens.onCyanFill,
                )
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .width(30.dp)
                .fillMaxHeight()
                .padding(vertical = 4.dp, horizontal = 2.dp)
                .sakugaCelShadow(tokens.celShadow, cornerRadius = 10.dp, offsetX = 2.dp, offsetY = 3.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .border(2.dp, tokens.inkBorder, RoundedCornerShape(10.dp))
                .onGloballyPositioned { coords -> railOrigin = coords.positionInRoot() }
                .onSizeChanged { railHeightPx = it.height.toFloat() }
                .semantics {
                    contentDescription = "Indeks cepat 0 sampai 9 dan A sampai Z"
                }
                .pointerInput(symbols) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()

                        fun resolveAt(y: Float) {
                            val h = size.height.toFloat().coerceAtLeast(1f)
                            val clamped = y.coerceIn(0f, h - 1f)
                            val idx = ((clamped / h) * symbols.size)
                                .toInt()
                                .coerceIn(0, symbols.lastIndex)
                            val chosen = symbols[idx]
                            dragYOffsetPx = clamped
                            if (activeDragSymbol != chosen) {
                                activeDragSymbol = chosen
                                onSymbolSelected(chosen)
                                fx.spawnTicks(
                                    center = Offset(railOrigin.x - 10f, railOrigin.y + clamped),
                                    count = 3,
                                    radiusDp = 12f,
                                    lenDp = 8f,
                                    dirRad = 3.14f,
                                    life = 0.26f,
                                )
                            }
                        }

                        resolveAt(down.position.y)

                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (change.pressed) {
                                change.consume()
                                resolveAt(change.position.y)
                            }
                        } while (event.changes.any { it.pressed })

                        activeDragSymbol = null
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            val slotHeightDp = maxHeight / symbols.size
            val fontSize = when {
                slotHeightDp >= 14.dp -> 9.5.sp
                slotHeightDp >= 11.dp -> 8.5.sp
                else -> 7.5.sp
            }
            val displayedSymbols = if (maxHeight >= 280.dp) {
                symbols
            } else {
                symbols.filterIndexed { index, ch -> ch == '0' || ch == '9' || index % 2 == 0 || ch == 'Z' }
            }

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 3.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val highlighted = activeDragSymbol ?: currentSymbol
                displayedSymbols.forEach { symbol ->
                    val isSelected = symbol == highlighted
                    val hasTracks = symbol in availableSymbols
                    Text(
                        text = symbol.toString(),
                        fontSize = fontSize,
                        lineHeight = fontSize,
                        fontWeight = if (isSelected) FontWeight.Black else if (hasTracks) FontWeight.Bold else FontWeight.Medium,
                        color = when {
                            isSelected -> tokens.accentText
                            hasTracks -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        },
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
