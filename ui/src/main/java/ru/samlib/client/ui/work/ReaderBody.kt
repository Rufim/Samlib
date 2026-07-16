package ru.samlib.client.ui.work

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import ru.samlib.client.ui.common.ErrorBox
import ru.samlib.client.ui.common.LoadingBox
import ru.samlib.client.ui.common.ReaderScrollbar

/**
 * Область чтения: список блоков, быстрый скролл-бар справа и «язычок» снизу,
 * открывающий текстовые настройки / шторку TTS / автопрокрутки.
 */
@Composable
fun ReaderBody(
    state: WorkState,
    onRetry: () -> Unit,
    listState: LazyListState,
    bg: Color,
    fg: Color,
    linkListener: LinkInteractionListener?,
    ttsActive: Boolean,
    ttsPlaying: Boolean,
    speakBlock: Int,
    wordStart: Int,
    wordEnd: Int,
    onWordTap: (index: Int, offset: Int) -> Unit,
    onActiveLayout: (TextLayoutResult) -> Unit,
    progress: Float,
    canScroll: Boolean,
    scrollbarVisible: Boolean,
    onTapPercent: () -> Unit,
    onScrollToFraction: (Float) -> Unit,
    autoScroll: Boolean,
    onOpenTtsSheet: () -> Unit,
    onOpenAutoScrollSheet: () -> Unit,
    onOpenTextSheet: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        when {
            state.loading -> LoadingBox()
            state.error != null -> ErrorBox(state.error, onRetry)
            else ->
                LazyColumn(
                    state = listState,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(bg),
                ) {
                    itemsIndexed(state.blocks) { index, block ->
                        val active = index == speakBlock
                        BlockView(
                            block = block,
                            style =
                                ReaderTextStyle(
                                    fontSizeSp = state.settings.fontSizeSp,
                                    font = state.settings.font,
                                    monoFont = state.settings.monoFont,
                                    textColor = fg,
                                    justify = state.settings.justify,
                                ),
                            highlight =
                                WordHighlight(
                                    wordStart = if (active) wordStart else -1,
                                    wordEnd = if (active) wordEnd else -1,
                                    color = Color(state.settings.highlightColor),
                                ),
                            linkListener = linkListener,
                            // при активной озвучке: тап по играющему тексту — стоп; следующий
                            // тап — старт с выбранной позиции. Шторку при этом не открываем.
                            onWordTap = { offset -> if (ttsActive) onWordTap(index, offset) },
                            onLayout = if (active) onActiveLayout else null,
                        )
                    }
                }
        }

        // Быстрый скролл-бар справа: появляется при открытии (если текст не влезает) и
        // при прокрутке (fade), показывает позицию (%), тап по бабблу — ввод процента.
        // Не показывается, если весь текст помещается на экране.
        val canShowBar = canScroll && state.error == null && !state.loading
        AnimatedVisibility(
            visible = scrollbarVisible && canShowBar,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            ReaderScrollbar(
                progress = progress,
                color = fg,
                onTapPercent = onTapPercent,
                onScrollToFraction = onScrollToFraction,
            )
        }

        // Язычок снизу: открывает шторку — TTS/автоскролл при активном режиме, иначе текст.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
                .width(120.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(fg.copy(alpha = 0.12f))
                .clickable {
                    when {
                        ttsActive -> onOpenTtsSheet()
                        autoScroll -> onOpenAutoScrollSheet()
                        else -> onOpenTextSheet()
                    }
                }.pointerInput(ttsActive, autoScroll) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount < -2f) {
                            when {
                                ttsActive -> onOpenTtsSheet()
                                autoScroll -> onOpenAutoScrollSheet()
                                else -> onOpenTextSheet()
                            }
                        }
                    }
                },
        )
    }
}
