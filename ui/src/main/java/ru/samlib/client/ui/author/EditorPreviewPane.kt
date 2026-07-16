package ru.samlib.client.ui.author

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import ru.samlib.client.domain.repository.DraftBlock
import ru.samlib.client.domain.settings.ReaderFont
import ru.samlib.client.ui.common.ReaderScrollbar
import ru.samlib.client.ui.common.TextPositionDialog
import ru.samlib.client.ui.work.BlockView
import ru.samlib.client.ui.work.ReaderTextStyle
import ru.samlib.client.ui.work.toUi

/** Превью черновика: список распарсенных блоков + скролл-бар с переходом по проценту. */
@Composable
fun BoxScope.EditorPreviewPane(
    draftBlocks: List<DraftBlock>,
    pendingOffset: Int,
    listState: LazyListState,
) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(draftBlocks) {
        val idx = draftBlockIndexForOffset(draftBlocks, pendingOffset)
        if (idx >= 0) listState.scrollToItem(idx)
    }
    val canScroll by remember {
        derivedStateOf { listState.canScrollForward || listState.canScrollBackward }
    }
    val progress by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            when {
                info.totalItemsCount == 0 -> 0f
                !listState.canScrollBackward -> 0f
                !listState.canScrollForward -> 1f
                else -> {
                    val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
                    ((lastVisible + 1).toFloat() / info.totalItemsCount).coerceIn(0f, 1f)
                }
            }
        }
    }

    fun scrollToFraction(f: Float) {
        val info = listState.layoutInfo
        val total = info.totalItemsCount
        if (total <= 0) return
        val exact = f.coerceIn(0f, 1f) * (total - 1)
        val index = exact.toInt().coerceIn(0, total - 1)
        val avgH =
            info.visibleItemsInfo
                .takeIf { it.isNotEmpty() }
                ?.map { it.size }
                ?.average() ?: 0.0
        val offset = ((exact - index) * avgH).toInt()
        scope.launch { listState.scrollToItem(index, offset) }
    }

    var showPercentDialog by remember { mutableStateOf(false) }
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        itemsIndexed(draftBlocks) { _, draftBlock ->
            BlockView(
                block = draftBlock.block.toUi(),
                style =
                    ReaderTextStyle(
                        fontSizeSp = 16,
                        font = ReaderFont.SERIF,
                        textColor = MaterialTheme.colorScheme.onSurface,
                    ),
            )
        }
    }
    if (canScroll) {
        ReaderScrollbar(
            progress = progress,
            color = MaterialTheme.colorScheme.onSurface,
            onTapPercent = { showPercentDialog = true },
            onScrollToFraction = { f -> scrollToFraction(f) },
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
    if (showPercentDialog) {
        TextPositionDialog(
            currentFraction = progress,
            onDismiss = { showPercentDialog = false },
            onGo = { f ->
                showPercentDialog = false
                scrollToFraction(f)
            },
        )
    }
}
