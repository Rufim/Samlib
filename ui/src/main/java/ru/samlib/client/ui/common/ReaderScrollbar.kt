package ru.samlib.client.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * Скролл-бар у правого края: рельс-линия у самого края (≈4dp), по нему ездит бегунок,
 * рельс проходит по центру бегунка; баббл с процентом — слева от бегунка.
 * Тап — [onTapPercent]; зажать и тянуть — перемотка [onScrollToFraction] как классический бегунок.
 * Общий для читалки ([ru.samlib.client.ui.work.ReaderScaffold]) и превью редактора текста.
 */
@Composable
fun ReaderScrollbar(
    progress: Float,
    color: Color,
    onTapPercent: () -> Unit,
    onScrollToFraction: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val thumbHeight = 34.dp
    var trackHeight by remember { mutableFloatStateOf(1f) }
    val thumbPx = with(density) { thumbHeight.toPx() }
    val curProgress by rememberUpdatedState(progress)

    fun yOffset() = IntOffset(0, (progress * trackHeight).toInt())

    val thumbGestures =
        Modifier
            .pointerInput(Unit) { detectTapGestures { onTapPercent() } }
            .pointerInput(Unit) {
                var acc = 0f
                detectVerticalDragGestures(
                    onDragStart = { acc = curProgress * trackHeight },
                    onVerticalDrag = { change, dy ->
                        change.consume()
                        acc = (acc + dy).coerceIn(0f, trackHeight)
                        onScrollToFraction(if (trackHeight > 0f) acc / trackHeight else 0f)
                    },
                )
            }

    Box(
        modifier
            .fillMaxHeight()
            .padding(top = 12.dp, bottom = 12.dp)
            .width(80.dp)
            .onSizeChanged { trackHeight = (it.height - thumbPx).coerceAtLeast(1f) },
    ) {
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 2.dp)
                .width(10.dp)
                .fillMaxHeight(),
        ) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(color.copy(alpha = 0.9f)),
            )
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .offset { yOffset() }
                    .width(10.dp)
                    .height(thumbHeight)
                    .clip(RoundedCornerShape(5.dp))
                    .background(color.copy(alpha = 0.9f))
                    .then(thumbGestures),
            )
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(end = 18.dp)
                .offset { yOffset() }
                .height(thumbHeight)
                .clip(RoundedCornerShape(17.dp))
                .background(color.copy(alpha = 0.9f))
                .then(thumbGestures)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "${(progress * 100).toInt()}%",
                color = MaterialTheme.colorScheme.surface,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
