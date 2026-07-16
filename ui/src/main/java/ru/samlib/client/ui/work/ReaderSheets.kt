package ru.samlib.client.ui.work

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.samlib.client.ui.R
import ru.samlib.client.ui.common.AppSlider

/** Оглавление книги (список заголовков-блоков), тап по пункту — переход к главе. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChaptersSheet(
    chapters: List<Pair<Int, String>>,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            stringResource(R.string.reader_toc),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyColumn(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp),
        ) {
            items(chapters) { (index, title) ->
                Text(
                    title,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(index) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                )
                HorizontalDivider()
            }
        }
    }
}

/** Управление озвучкой: play/pause, стоп, скорость и тембр речи. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TtsControlSheet(
    playing: Boolean,
    rate: Float,
    pitch: Float,
    onDismiss: () -> Unit,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onRateChange: (Float) -> Unit,
    onRateChangeFinished: () -> Unit,
    onPitchChange: (Float) -> Unit,
    onPitchChangeFinished: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPlayPause) {
                    if (playing) {
                        Icon(
                            painterResource(R.drawable.ic_pause),
                            contentDescription = stringResource(R.string.reader_pause),
                        )
                    } else {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.reader_resume),
                        )
                    }
                }
                Text(stringResource(R.string.reader_tts), Modifier.weight(1f))
                IconButton(onClick = onStop) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.reader_stop_tts),
                    )
                }
            }
            // слайдеры друг под другом (стопкой), а не рядом; параметры применяются на лету
            Text(
                stringResource(R.string.reader_tts_rate, "%.1f".format(rate)),
                Modifier.padding(top = 8.dp),
            )
            AppSlider(
                value = rate,
                onValueChange = onRateChange,
                // по отпусканию — прервать и перечитать текущую фразу с новой скоростью
                onValueChangeFinished = onRateChangeFinished,
                valueRange = 0.5f..2.0f,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(R.string.reader_tts_pitch, "%.1f".format(pitch)),
                Modifier.padding(top = 8.dp),
            )
            AppSlider(
                value = pitch,
                onValueChange = onPitchChange,
                onValueChangeFinished = onPitchChangeFinished,
                valueRange = 0.5f..2.0f,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Управление автопрокруткой: стоп и скорость. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoScrollSheet(
    speed: Int,
    onDismiss: () -> Unit,
    onStop: () -> Unit,
    onSpeedChange: (Int) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.reader_autoscroll), Modifier.weight(1f))
                IconButton(onClick = onStop) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.reader_stop_autoscroll),
                    )
                }
            }
            Text(
                stringResource(R.string.reader_autoscroll_speed, speed),
                Modifier.padding(top = 8.dp),
            )
            AppSlider(
                value = speed.toFloat(),
                onValueChange = { onSpeedChange(it.toInt()) },
                valueRange = 1f..10f,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
