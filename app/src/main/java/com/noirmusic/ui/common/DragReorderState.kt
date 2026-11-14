package com.noirmusic.ui.common

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay

/** Holds drag-to-reorder state for a [LazyColumn] whose items match indices `0 until itemCount`. */
class DragReorderState(
    val listState: LazyListState,
    private val onMoveProvider: () -> ((Int, Int) -> Unit),
    private val onCommitMoveProvider: () -> ((Int, Int) -> Unit),
) {
    var draggedKey by mutableStateOf<Any?>(null)
        private set
    var draggingOffsetY by mutableFloatStateOf(0f)
        private set
    private var startIndex by mutableIntStateOf(-1)
    private var currentIndex by mutableIntStateOf(-1)
    var autoScrollVelocity by mutableFloatStateOf(0f)
        private set

    fun onDragStart(key: Any) {
        val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } ?: return
        draggedKey = key
        startIndex = info.index
        currentIndex = info.index
        draggingOffsetY = 0f
        autoScrollVelocity = 0f
    }

    fun onDrag(deltaY: Float) {
        val key = draggedKey ?: return
        draggingOffsetY += deltaY
        checkSwapAndScroll(key)
    }

    fun onDragStepAutoScroll(scrolledDelta: Float) {
        val key = draggedKey ?: return
        if (scrolledDelta != 0f) {
            draggingOffsetY += scrolledDelta
            checkSwapAndScroll(key)
        }
    }

    private fun checkSwapAndScroll(key: Any) {
        val visible = listState.layoutInfo.visibleItemsInfo
        val draggedInfo = visible.firstOrNull { it.key == key } ?: return
        val draggedTop = draggedInfo.offset + draggingOffsetY
        val draggedBottom = draggedTop + draggedInfo.size
        val draggedCenter = draggedTop + draggedInfo.size / 2f

        val nextInfo = visible.firstOrNull { it.index == currentIndex + 1 }
        val prevInfo = visible.firstOrNull { it.index == currentIndex - 1 }

        if (nextInfo != null && draggedCenter > nextInfo.offset + nextInfo.size / 2f) {
            onMoveProvider().invoke(currentIndex, currentIndex + 1)
            draggingOffsetY -= (nextInfo.offset - draggedInfo.offset)
            currentIndex += 1
        } else if (prevInfo != null && draggedCenter < prevInfo.offset + prevInfo.size / 2f) {
            onMoveProvider().invoke(currentIndex, currentIndex - 1)
            draggingOffsetY += (draggedInfo.offset - prevInfo.offset)
            currentIndex -= 1
        }

        val viewportStart = listState.layoutInfo.viewportStartOffset.toFloat()
        val viewportEnd = listState.layoutInfo.viewportEndOffset.toFloat()
        val edgeThreshold = 72f
        autoScrollVelocity = when {
            draggedBottom > viewportEnd - edgeThreshold ->
                ((draggedBottom - (viewportEnd - edgeThreshold)) / edgeThreshold).coerceIn(0.2f, 1.5f) * 18f
            draggedTop < viewportStart + edgeThreshold ->
                -((viewportStart + edgeThreshold - draggedTop) / edgeThreshold).coerceIn(0.2f, 1.5f) * 18f
            else -> 0f
        }
    }

    fun onDragEnd() {
        val from = startIndex
        val to = currentIndex
        draggedKey = null
        draggingOffsetY = 0f
        startIndex = -1
        currentIndex = -1
        autoScrollVelocity = 0f
        if (from >= 0 && to >= 0 && from != to) {
            onCommitMoveProvider().invoke(from, to)
        }
    }
}

@Composable
fun rememberDragReorderState(
    listState: LazyListState,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onCommitMove: (fromIndex: Int, toIndex: Int) -> Unit,
): DragReorderState {
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnCommitMove by rememberUpdatedState(onCommitMove)
    val state = remember(listState) {
        DragReorderState(
            listState = listState,
            onMoveProvider = { currentOnMove },
            onCommitMoveProvider = { currentOnCommitMove },
        )
    }

    LaunchedEffect(state.draggedKey, state.autoScrollVelocity) {
        if (state.draggedKey != null && state.autoScrollVelocity != 0f) {
            while (state.draggedKey != null && state.autoScrollVelocity != 0f) {
                val consumed = state.listState.scrollBy(state.autoScrollVelocity)
                if (consumed == 0f) break
                state.onDragStepAutoScroll(consumed)
                delay(16L)
            }
        }
    }

    return state
}

fun Modifier.dragHandle(
    state: DragReorderState,
    key: Any,
): Modifier = pointerInput(state, key) {
    detectDragGestures(
        onDragStart = { state.onDragStart(key) },
        onDrag = { change, dragAmount ->
            change.consume()
            state.onDrag(dragAmount.y)
        },
        onDragEnd = { state.onDragEnd() },
        onDragCancel = { state.onDragEnd() },
    )
}
