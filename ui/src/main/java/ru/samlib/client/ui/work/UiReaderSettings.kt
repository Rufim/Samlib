package ru.samlib.client.ui.work

import androidx.compose.runtime.Immutable
import ru.samlib.client.domain.settings.ReaderFont
import ru.samlib.client.domain.settings.ReaderSettings
import ru.samlib.client.domain.settings.ReaderTheme

/** Стабильная для Compose-рекомпозиции копия [ReaderSettings]. */
@Immutable
data class UiReaderSettings(
    val fontSizeSp: Int = 18,
    val font: ReaderFont = ReaderFont.SERIF,
    val monoFont: ReaderFont = ReaderFont.MONO,
    val theme: ReaderTheme = ReaderTheme.SYSTEM,
    val justify: Boolean = true,
    val bgColor: Int? = null,
    val textColor: Int? = null,
    val highlightColor: Int = 0x8081D4FA.toInt(),
    val autoScrollSpeed: Int = 3,
    val ttsRate: Float = 1.0f,
    val ttsPitch: Float = 1.0f,
    val ttsEngine: String? = null,
    val ttsVoice: String? = null,
    val voiceLanguage: String = "ru",
    val translateTarget: String = "ru",
    val translateSource: String? = null,
    val sleepTimerMinutes: Int = 0,
)

fun ReaderSettings.toUi(): UiReaderSettings =
    UiReaderSettings(
        fontSizeSp = fontSizeSp,
        font = font,
        monoFont = monoFont,
        theme = theme,
        justify = justify,
        bgColor = bgColor,
        textColor = textColor,
        highlightColor = highlightColor,
        autoScrollSpeed = autoScrollSpeed,
        ttsRate = ttsRate,
        ttsPitch = ttsPitch,
        ttsEngine = ttsEngine,
        ttsVoice = ttsVoice,
        voiceLanguage = voiceLanguage,
        translateTarget = translateTarget,
        translateSource = translateSource,
        sleepTimerMinutes = sleepTimerMinutes,
    )
