package com.noirmusic.ui.library

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noirmusic.data.media.TrackListLogic
import kotlin.math.roundToInt

/**
 * Right-edge fast scroller rail displaying digits (0-9) and letters (A-Z).
 * Supports both single tap and vertical drag scrubbing to jump across large libraries.
 */
@Composable
fun AlphabetFastScroller(
    availableSymbols: Set<Char>,
    currentSymbol: Char?,
    onSymbolSelected: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    val symbols = TrackListLogic.FAST_SCROLL_SYMBOLS
    var railHeightPx by remember { mutableFloatStateOf(0f) }
    var activeDragSymbol by remember { mutableStateOf<Char?>(null) }
    var dragYOffsetPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.CenterEnd,
    ) {
        // Floating preview indicator shown to the left of the rail while dragging/tapping
        val previewChar = activeDragSymbol
        if (previewChar != null && railHeightPx > 0f) {
            val badgeSizePx = with(density) { 48.dp.toPx() }
            val clampedY = (dragYOffsetPx - badgeSizePx / 2f)
                .coerceIn(0f, (railHeightPx - badgeSizePx).coerceAtLeast(0f))

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 36.dp)
                    .offset { IntOffset(0, clampedY.roundToInt()) }
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(10.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = previewChar.toString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        // Tactile index rail (0-9, A-Z)
        BoxWithConstraints(
            modifier = Modifier
                .width(28.dp)
                .fillMaxHeight()
                .padding(vertical = 4.dp, horizontal = 2.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(8.dp),
                )
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
            // Show all 36 symbols when height allows (>= 280.dp); on very compact heights (e.g. keyboard open),
            // thin displayed labels while keeping touch resolution across all 36 symbols.
            val displayedSymbols = if (maxHeight >= 280.dp) {
                symbols
            } else {
                symbols.filterIndexed { index, ch -> ch == '0' || ch == '9' || index % 2 == 0 || ch == 'Z' }
            }

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 2.dp),
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
                        fontWeight = if (isSelected) FontWeight.Bold else if (hasTracks) FontWeight.SemiBold else FontWeight.Normal,
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            hasTracks -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.outline
                        },
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
