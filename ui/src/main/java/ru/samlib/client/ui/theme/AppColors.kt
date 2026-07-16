package ru.samlib.client.ui.theme

import androidx.compose.ui.graphics.Color
import ru.samlib.client.domain.settings.ReaderTheme

/** Все фирменные цвета приложения вне Material-темы — единственный источник. */
object AppColors {
    // Темы читалки: фон / текст
    val ReaderLightBg = Color(0xFFFFFFFF)
    val ReaderLightFg = Color(0xFF111111)
    val ReaderSepiaBg = Color(0xFFF4ECD8)
    val ReaderSepiaFg = Color(0xFF5B4636)
    val ReaderDarkBg = Color(0xFF121212)
    val ReaderDarkFg = Color(0xFFE0E0E0)

    /** Подсветка озвучиваемого слова по умолчанию (полупрозрачный голубой). */
    val TtsHighlightDefault = Color(0x8081D4FA)

    /** Подсветка совпадений поиска в редакторе: текущее ярче остальных. */
    val SearchHitCurrent = Color(0xB4FF9800)
    val SearchHitOther = Color(0x64FFEB3B)

    /** Успешное состояние (зелёная отметка «онлайн» на главной). */
    val Success = Color(0xFF2E7D32)

    /** Закреплённый элемент истории. */
    val PinnedBlue = Color(0xFF2962FF)
}

/** Пара фон/текст читалки для выбранной темы. */
fun ReaderTheme.colors(dark: Boolean): Pair<Color, Color> =
    when (this) {
        ReaderTheme.LIGHT -> AppColors.ReaderLightBg to AppColors.ReaderLightFg
        ReaderTheme.SEPIA -> AppColors.ReaderSepiaBg to AppColors.ReaderSepiaFg
        ReaderTheme.DARK -> AppColors.ReaderDarkBg to AppColors.ReaderDarkFg
        ReaderTheme.SYSTEM ->
            if (dark) {
                AppColors.ReaderDarkBg to AppColors.ReaderDarkFg
            } else {
                AppColors.ReaderLightBg to AppColors.ReaderLightFg
            }
    }
