package ru.samlib.client.domain.settings

enum class ReaderFont(
    val title: String,
    val asset: String?,
    val monospace: Boolean = false,
) {
    SERIF("Serif", null),
    SANS("Sans", null),
    MONO("Mono", null, monospace = true),
    ARIAL("Arial", "fonts/arial.ttf"),
    ARIAL_BLACK("Arial Black", "fonts/arial_black.ttf"),
    GEORGIA("Georgia", "fonts/georgia.ttf"),
    TIMES("Times New Roman", "fonts/times_new_roman.ttf"),
    COURIER("Courier New", "fonts/courier_new.ttf", monospace = true),
    COMIC("Comic Sans", "fonts/comic_sans.ttf"),
    MS_SANS("MS Sans Serif", "fonts/ms_sans_serif.ttf"),
    ANDALE("Andale Mono", "fonts/andale_mono.ttf", monospace = true),
    IZHITSA("Ижица", "fonts/izhitsa.ttf"),
    JAKOB("Якоб", "fonts/jakob.ttf"),
    TORHOK("Торжок", "fonts/torhok.ttf"),
}

enum class ReaderTheme { SYSTEM, LIGHT, SEPIA, DARK }

/** Стабильная для рекомпозиции копия — `ru.samlib.client.ui.work.UiReaderSettings` (`:ui`). */
data class ReaderSettings(
    val fontSizeSp: Int = 18,
    val font: ReaderFont = ReaderFont.SERIF,
    /** Шрифт моноширинных блоков (pre) — выбирается из моноширинных отдельно от основного. */
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
    /** Принудительный голос озвучки; null — авто: голос подбирается под язык текста на лету. */
    val ttsVoice: String? = null,
    /** Язык, которым фильтруется список голосов в настройках (BCP-47); по умолчанию русский. */
    val voiceLanguage: String = "ru",
    /** Язык перевода — назначение (BCP-47) для ML Kit. */
    val translateTarget: String = "ru",
    /**
     * Язык перевода — источник (BCP-47); null — автоопределение через ML Kit (по умолчанию).
     * Если задан явно, перевод идёт с этого языка без вызова детектора.
     */
    val translateSource: String? = null,
    /** Таймер сна для озвучки, минуты (0 = выключен). */
    val sleepTimerMinutes: Int = 0,
)
