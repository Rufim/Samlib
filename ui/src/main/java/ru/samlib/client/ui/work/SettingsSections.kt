package ru.samlib.client.ui.work

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.samlib.client.domain.settings.ReaderFont
import ru.samlib.client.domain.settings.ReaderTheme
import ru.samlib.client.ui.R
import ru.samlib.client.ui.common.AppSlider
import ru.samlib.client.ui.theme.AppColors
import java.util.Locale

/** Цвет подсветки читаемого голосом текста по умолчанию (единый источник — AppColors). */
val DEFAULT_HIGHLIGHT_COLOR: Int = AppColors.TtsHighlightDefault.toArgb()

/** Заголовок категории настроек с разделителем сверху. */
@Composable
fun SettingsCategory(
    title: String,
    first: Boolean = false,
) {
    if (!first) HorizontalDivider(Modifier.padding(vertical = 14.dp))
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
fun FontSizeSection(
    settings: UiReaderSettings,
    onFontSize: (Int) -> Unit,
) {
    Text(stringResource(R.string.settings_font_size, settings.fontSizeSp))
    AppSlider(
        value = settings.fontSizeSp.toFloat(),
        onValueChange = { onFontSize(it.toInt()) },
        valueRange = 12f..30f,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun FontSection(
    settings: UiReaderSettings,
    onFont: (ReaderFont) -> Unit,
) {
    Text(stringResource(R.string.settings_font))
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        ReaderFont.entries.forEach { f ->
            FilterChip(
                selected = settings.font == f,
                onClick = { onFont(f) },
                label = { Text(f.title) },
                modifier = Modifier.padding(end = 8.dp),
            )
        }
    }
}

@Composable
fun MonoFontSection(
    settings: UiReaderSettings,
    onMonoFont: (ReaderFont) -> Unit,
) {
    Text(stringResource(R.string.settings_mono_font), Modifier.padding(top = 8.dp))
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        ReaderFont.entries.filter { it.monospace }.forEach { f ->
            FilterChip(
                selected = settings.monoFont == f,
                onClick = { onMonoFont(f) },
                label = { Text(f.title) },
                modifier = Modifier.padding(end = 8.dp),
            )
        }
    }
}

/** Общий виджет выбора языка (BCP-47): кнопка с текущим языком, по клику — диалог со списком. */
@Composable
private fun LanguagePickerRow(
    label: String,
    current: String,
    languages: List<String>,
    onSelect: (String) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        OutlinedButton(onClick = { showDialog = true }) {
            Text(languageDisplayName(current).replaceFirstChar(Char::uppercase))
        }
    }
    if (showDialog) {
        val items =
            remember(languages) {
                languages
                    .map { it to languageDisplayName(it).replaceFirstChar(Char::uppercase) }
                    .sortedBy { it.second }
            }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                }) { Text(stringResource(R.string.settings_close)) }
            },
            title = { Text(label) },
            text = {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(items.size) { i ->
                        val (code, name) = items[i]
                        Text(
                            if (code == current) "✓ $name" else name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (code == current) MaterialTheme.colorScheme.primary else Color.Unspecified,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showDialog = false
                                        onSelect(code)
                                    }.padding(vertical = 10.dp, horizontal = 4.dp),
                        )
                    }
                }
            },
        )
    }
}

/**
 * Направление перевода: язык-назначение — всегда явный выбор; язык-источник — переключатель
 * «авто» (ML Kit определяет по тексту, как раньше) / «вручную» (явный выбор без детектора).
 */
@Composable
fun TranslateLanguageSection(
    settings: UiReaderSettings,
    languages: List<String>,
    onTranslateTarget: (String) -> Unit,
    onTranslateSource: (String?) -> Unit,
) {
    LanguagePickerRow(
        label = stringResource(R.string.settings_translate_language),
        current = settings.translateTarget,
        languages = languages,
        onSelect = onTranslateTarget,
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.settings_translate_source_auto))
        Switch(
            checked = settings.translateSource == null,
            onCheckedChange = { auto ->
                onTranslateSource(
                    if (auto) {
                        null
                    } else {
                        settings.translateSource
                            ?: if (settings.translateTarget != "en") "en" else "ru"
                    },
                )
            },
        )
    }
    val source = settings.translateSource
    if (source != null) {
        LanguagePickerRow(
            label = stringResource(R.string.settings_translate_source),
            current = source,
            languages = languages,
            onSelect = { onTranslateSource(it) },
        )
    }
}

/** Язык, которым фильтруется список голосов в "Озвучке" (см. [UiReaderSettings.voiceLanguage]). */
@Composable
fun VoiceLanguageSection(
    settings: UiReaderSettings,
    languages: List<String>,
    onVoiceLanguage: (String) -> Unit,
) {
    LanguagePickerRow(
        label = stringResource(R.string.settings_voice_language),
        current = settings.voiceLanguage,
        languages = languages,
        onSelect = onVoiceLanguage,
    )
}

/** Название языка по BCP-47 на русском (не зависит от локали системы). */
private fun languageDisplayName(code: String): String = Locale(code).getDisplayLanguage(Locale("ru")).ifBlank { code }

@Composable
fun JustifySection(
    settings: UiReaderSettings,
    onJustify: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.settings_justify))
        Switch(checked = settings.justify, onCheckedChange = onJustify)
    }
}

@Composable
fun NetworkSection(
    domain: String,
    proxy: String,
    onApply: (domain: String, proxy: String) -> Unit,
) {
    var d by remember(domain) { mutableStateOf(domain) }
    var p by remember(proxy) { mutableStateOf(proxy) }
    OutlinedTextField(
        value = d,
        onValueChange = { d = it },
        label = { Text(stringResource(R.string.settings_domain_label)) },
        placeholder = { Text("http://samlib.ru") },
        singleLine = true,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
    )
    OutlinedTextField(
        value = p,
        onValueChange = { p = it },
        label = { Text(stringResource(R.string.settings_proxy_label)) },
        placeholder = { Text(stringResource(R.string.settings_proxy_hint)) },
        singleLine = true,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
    )
    OutlinedButton(onClick = { onApply(d, p) }, modifier = Modifier.padding(top = 4.dp)) {
        Text(stringResource(R.string.settings_apply_network))
    }
}

@Composable
fun OfflineSection(
    offlineForced: Boolean,
    onOffline: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.settings_offline))
            Text(
                stringResource(R.string.settings_offline_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = offlineForced, onCheckedChange = onOffline)
    }
}

@Composable
fun LoggingSection(
    fileLogging: Boolean,
    onFileLogging: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.settings_file_logging))
            Text(
                stringResource(R.string.settings_file_logging_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = fileLogging, onCheckedChange = onFileLogging)
    }
}

@Composable
fun ThemeSection(
    settings: UiReaderSettings,
    onTheme: (ReaderTheme) -> Unit,
) {
    Text(stringResource(R.string.settings_theme), Modifier.padding(top = 8.dp))
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        ReaderTheme.entries.forEach { t ->
            FilterChip(
                selected = settings.theme == t,
                onClick = { onTheme(t) },
                label = { Text(t.name) },
                modifier = Modifier.padding(end = 8.dp),
            )
        }
    }
}

@Composable
fun ColorsSection(
    settings: UiReaderSettings,
    onBgColor: (Int?) -> Unit,
    onTextColor: (Int?) -> Unit,
) {
    var picker by remember { mutableStateOf<PickerTarget?>(null) }

    Text(stringResource(R.string.settings_colors), Modifier.padding(top = 8.dp))
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ColorButton(stringResource(R.string.settings_bg), settings.bgColor) {
            picker = PickerTarget.BG
        }
        ColorButton(stringResource(R.string.settings_text), settings.textColor) {
            picker = PickerTarget.TEXT
        }
        TextButton(onClick = {
            onBgColor(null)
            onTextColor(null)
        }) { Text(stringResource(R.string.settings_reset)) }
    }

    picker?.let { target ->
        val initial =
            when (target) {
                PickerTarget.BG -> settings.bgColor ?: Color.White.toArgb()
                PickerTarget.TEXT -> settings.textColor ?: Color.Black.toArgb()
            }
        ColorPickerDialog(
            initial = initial,
            onPick = { argb ->
                when (target) {
                    PickerTarget.BG -> onBgColor(argb)
                    PickerTarget.TEXT -> onTextColor(argb)
                }
                picker = null
            },
            onDismiss = { picker = null },
        )
    }
}

@Composable
fun HighlightColorSection(
    settings: UiReaderSettings,
    onHighlightColor: (Int) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }

    Text(stringResource(R.string.settings_highlight), Modifier.padding(top = 8.dp))
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ColorButton(stringResource(R.string.settings_color), settings.highlightColor) {
            showPicker = true
        }
        TextButton(
            onClick = { onHighlightColor(DEFAULT_HIGHLIGHT_COLOR) },
        ) { Text(stringResource(R.string.settings_reset)) }
    }

    if (showPicker) {
        ColorPickerDialog(
            initial = settings.highlightColor,
            onPick = { argb ->
                onHighlightColor(argb)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun ColorButton(
    label: String,
    color: Int?,
    onClick: () -> Unit,
) {
    OutlinedButton(onClick = onClick) {
        Box(
            Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(color?.let { Color(it) } ?: Color.Transparent)
                .padding(end = 6.dp),
        )
        Spacer(Modifier.size(6.dp))
        Text(label)
    }
}

@Composable
fun AutoScrollSection(
    settings: UiReaderSettings,
    onAutoSpeed: (Int) -> Unit,
) {
    Text(
        stringResource(R.string.settings_autoscroll_speed, settings.autoScrollSpeed),
        Modifier.padding(top = 8.dp),
    )
    AppSlider(
        value = settings.autoScrollSpeed.toFloat(),
        onValueChange = { onAutoSpeed(it.toInt()) },
        valueRange = 1f..10f,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun SleepTimerSection(
    settings: UiReaderSettings,
    onSleepTimer: (Int) -> Unit,
) {
    val m = settings.sleepTimerMinutes
    Text(
        if (m == 0) {
            stringResource(R.string.settings_sleep_timer_off)
        } else {
            stringResource(
                R.string.settings_sleep_timer_min,
                m,
            )
        },
        Modifier.padding(top = 8.dp),
    )
    AppSlider(
        value = m.toFloat(),
        onValueChange = { onSleepTimer(it.toInt()) },
        valueRange = 0f..90f,
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        stringResource(R.string.settings_sleep_timer_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun TtsSpeedSection(
    settings: UiReaderSettings,
    onTtsRate: (Float) -> Unit,
    onTtsPitch: (Float) -> Unit,
) {
    Text(
        stringResource(R.string.settings_tts_rate, "%.1f".format(settings.ttsRate)),
        Modifier.padding(top = 8.dp),
    )
    AppSlider(
        value = settings.ttsRate,
        onValueChange = onTtsRate,
        valueRange = 0.5f..2.0f,
        modifier = Modifier.fillMaxWidth(),
    )
    Text(stringResource(R.string.settings_tts_pitch, "%.1f".format(settings.ttsPitch)))
    AppSlider(
        value = settings.ttsPitch,
        onValueChange = onTtsPitch,
        valueRange = 0.5f..2.0f,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun TtsInfoNotice(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
    )
}

@Composable
fun TtsEngineSection(
    ttsEngines: List<Pair<String, String>>,
    currentEngine: String?,
    onTtsEngine: (String?) -> Unit,
) {
    if (ttsEngines.isEmpty()) return
    Text(stringResource(R.string.settings_tts_engine), Modifier.padding(top = 8.dp))
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        FilterChip(
            selected = currentEngine == null,
            onClick = { onTtsEngine(null) },
            label = { Text(stringResource(R.string.settings_default)) },
            modifier = Modifier.padding(end = 8.dp),
        )
        ttsEngines.forEach { (pkg, label) ->
            FilterChip(
                selected = currentEngine == pkg,
                onClick = { onTtsEngine(pkg) },
                label = { Text(label) },
                modifier = Modifier.padding(end = 8.dp),
            )
        }
    }
}

@Composable
fun TtsVoiceSection(
    ttsVoices: List<String>,
    currentVoice: String?,
    onTtsVoice: (String?) -> Unit,
) {
    if (ttsVoices.isEmpty()) return
    Text(stringResource(R.string.settings_tts_voice), Modifier.padding(top = 8.dp))
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        FilterChip(
            selected = currentVoice == null,
            onClick = { onTtsVoice(null) },
            label = { Text(stringResource(R.string.settings_auto)) },
            modifier = Modifier.padding(end = 8.dp),
        )
        ttsVoices.forEach { v ->
            FilterChip(
                selected = currentVoice == v,
                onClick = { onTtsVoice(v) },
                label = { Text(v) },
                modifier = Modifier.padding(end = 8.dp),
            )
        }
    }
}

private enum class PickerTarget { BG, TEXT }

@Composable
private fun ColorPickerDialog(
    initial: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val hsv =
        remember {
            FloatArray(3).also { android.graphics.Color.colorToHSV(initial, it) }
        }
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var sat by remember { mutableFloatStateOf(hsv[1]) }
    var value by remember { mutableFloatStateOf(hsv[2]) }
    val color = Color.hsv(hue, sat, value)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onPick(color.toArgb()) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_cancel)) } },
        title = { Text(stringResource(R.string.settings_pick_color)) },
        text = {
            Column {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color),
                ) {}
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.settings_hue))
                Slider(value = hue, onValueChange = { hue = it }, valueRange = 0f..360f)
                Text(stringResource(R.string.settings_saturation))
                Slider(value = sat, onValueChange = { sat = it }, valueRange = 0f..1f)
                Text(stringResource(R.string.settings_brightness))
                Slider(value = value, onValueChange = { value = it }, valueRange = 0f..1f)
            }
        },
    )
}
