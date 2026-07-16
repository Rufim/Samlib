package ru.samlib.client.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/** Слайдер с круглым ползунком без белого зазора вокруг него. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    // Локальная позиция: значение из настроек приходит асинхронно (DataStore), поэтому
    // при перетаскивании двигаем ползунок сразу, а внешние изменения подхватываем.
    var pos by remember { mutableFloatStateOf(value) }
    LaunchedEffect(value) { pos = value }
    Slider(
        value = pos,
        onValueChange = {
            pos = it
            onValueChange(it)
        },
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        modifier = modifier,
        thumb = {
            Box(
                Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        },
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                thumbTrackGapSize = 0.dp,
                drawStopIndicator = null,
            )
        },
    )
}
