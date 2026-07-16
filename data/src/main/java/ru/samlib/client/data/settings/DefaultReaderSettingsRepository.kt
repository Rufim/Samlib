package ru.samlib.client.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ru.samlib.client.domain.repository.ReaderSettingsRepository
import ru.samlib.client.domain.settings.NetworkSettings
import ru.samlib.client.domain.settings.ReaderFont
import ru.samlib.client.domain.settings.ReaderSettings
import ru.samlib.client.domain.settings.ReaderTheme

class DefaultReaderSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : ReaderSettingsRepository {
    private val fontSizeKey = intPreferencesKey("font_size")
    private val fontKey = stringPreferencesKey("font")
    private val monoFontKey = stringPreferencesKey("mono_font")
    private val themeKey = stringPreferencesKey("theme")
    private val justifyKey = booleanPreferencesKey("justify")
    private val bgColorKey = intPreferencesKey("bg_color")
    private val textColorKey = intPreferencesKey("text_color")
    private val highlightColorKey = intPreferencesKey("highlight_color")
    private val autoScrollSpeedKey = intPreferencesKey("auto_scroll_speed")
    private val ttsRateKey = floatPreferencesKey("tts_rate")
    private val ttsPitchKey = floatPreferencesKey("tts_pitch")
    private val ttsEngineKey = stringPreferencesKey("tts_engine")
    private val ttsVoiceKey = stringPreferencesKey("tts_voice")
    private val voiceLanguageKey = stringPreferencesKey("voice_language")
    private val translateTargetKey = stringPreferencesKey("translate_target")
    private val translateSourceKey = stringPreferencesKey("translate_source")
    private val sleepTimerKey = intPreferencesKey("sleep_timer_minutes")
    private val offlineForcedKey = booleanPreferencesKey("offline_forced")
    private val samlibDomainKey = stringPreferencesKey("samlib_domain")
    private val proxyKey = stringPreferencesKey("proxy")
    private val fileLoggingKey = booleanPreferencesKey("file_logging")

    // Последняя открытая книга: "work:<link>" или "local:<uri>". Для авто-открытия при старте.
    private val lastBookKey = stringPreferencesKey("last_book")

    // Однократный перенос данных из старой (legacy) БД выполнен.
    private val legacyImportDoneKey = booleanPreferencesKey("legacy_import_done")

    /** Был ли уже выполнен перенос из legacy-БД. */
    override suspend fun legacyImportDoneOnce(): Boolean = dataStore.data.first()[legacyImportDoneKey] ?: false

    override suspend fun setLegacyImportDone() {
        dataStore.edit { it[legacyImportDoneKey] = true }
    }

    /** Последняя открытая книга (или null) — прочитать один раз при старте. */
    override suspend fun lastBookOnce(): String? = dataStore.data.first()[lastBookKey]?.ifBlank { null }

    override suspend fun setLastBook(value: String) {
        dataStore.edit { it[lastBookKey] = value }
    }

    override suspend fun clearLastBook() {
        dataStore.edit { it.remove(lastBookKey) }
    }

    /** Принудительный офлайн-режим (переключатель в настройках). */
    override val offlineForced: Flow<Boolean> = dataStore.data.map { it[offlineForcedKey] ?: false }

    override suspend fun setOfflineForced(value: Boolean) {
        dataStore.edit { it[offlineForcedKey] = value }
    }

    /** Запись лога приложения в файл в Download (переключатель в настройках). */
    override val fileLogging: Flow<Boolean> = dataStore.data.map { it[fileLoggingKey] ?: false }

    override suspend fun setFileLogging(value: Boolean) {
        dataStore.edit { it[fileLoggingKey] = value }
    }

    /** Домен samlib (пусто = по умолчанию http://samlib.ru) и HTTP-прокси «host:port». */
    override val network: Flow<NetworkSettings> =
        dataStore.data.map { p ->
            NetworkSettings(
                domain = p[samlibDomainKey].orEmpty(),
                proxy = p[proxyKey].orEmpty(),
            )
        }

    override suspend fun setSamlibDomain(domain: String) {
        dataStore.edit { it[samlibDomainKey] = domain.trim() }
    }

    override suspend fun setProxy(proxy: String) {
        dataStore.edit { it[proxyKey] = proxy.trim() }
    }

    override val settings: Flow<ReaderSettings> =
        dataStore.data.map { p ->
            ReaderSettings(
                fontSizeSp = p[fontSizeKey] ?: 18,
                font =
                    p[fontKey]?.let { runCatching { ReaderFont.valueOf(it) }.getOrNull() }
                        ?: ReaderFont.SERIF,
                monoFont =
                    p[monoFontKey]
                        ?.let { runCatching { ReaderFont.valueOf(it) }.getOrNull() }
                        ?.takeIf { it.monospace } ?: ReaderFont.MONO,
                theme =
                    p[themeKey]?.let { runCatching { ReaderTheme.valueOf(it) }.getOrNull() }
                        ?: ReaderTheme.SYSTEM,
                justify = p[justifyKey] ?: true,
                bgColor = p[bgColorKey],
                textColor = p[textColorKey],
                highlightColor = p[highlightColorKey] ?: 0x8081D4FA.toInt(),
                autoScrollSpeed = (p[autoScrollSpeedKey] ?: 3).coerceIn(1, 10),
                ttsRate = (p[ttsRateKey] ?: 1.0f).coerceIn(0.5f, 2.0f),
                ttsPitch = (p[ttsPitchKey] ?: 1.0f).coerceIn(0.5f, 2.0f),
                ttsEngine = p[ttsEngineKey],
                ttsVoice = p[ttsVoiceKey],
                voiceLanguage = p[voiceLanguageKey] ?: "ru",
                translateTarget = p[translateTargetKey] ?: "ru",
                translateSource = p[translateSourceKey],
                sleepTimerMinutes = (p[sleepTimerKey] ?: 0).coerceIn(0, 90),
            )
        }

    override suspend fun setSleepTimer(minutes: Int) {
        dataStore.edit { it[sleepTimerKey] = minutes.coerceIn(0, 90) }
    }

    override suspend fun setAutoScrollSpeed(speed: Int) {
        dataStore.edit { it[autoScrollSpeedKey] = speed.coerceIn(1, 10) }
    }

    override suspend fun setTtsRate(rate: Float) {
        dataStore.edit { it[ttsRateKey] = rate.coerceIn(0.5f, 2.0f) }
    }

    override suspend fun setTtsPitch(pitch: Float) {
        dataStore.edit { it[ttsPitchKey] = pitch.coerceIn(0.5f, 2.0f) }
    }

    override suspend fun setTtsEngine(engine: String?) {
        dataStore.edit {
            if (engine == null) it.remove(ttsEngineKey) else it[ttsEngineKey] = engine
        }
    }

    override suspend fun setTtsVoice(voice: String?) {
        dataStore.edit { if (voice == null) it.remove(ttsVoiceKey) else it[ttsVoiceKey] = voice }
    }

    override suspend fun setVoiceLanguage(lang: String) {
        dataStore.edit { it[voiceLanguageKey] = lang }
    }

    override suspend fun setTranslateTarget(lang: String) {
        dataStore.edit { it[translateTargetKey] = lang }
    }

    override suspend fun setTranslateSource(lang: String?) {
        dataStore.edit { if (lang == null) it.remove(translateSourceKey) else it[translateSourceKey] = lang }
    }

    override suspend fun setFontSize(size: Int) {
        val clamped = size.coerceIn(12, 30)
        dataStore.edit { it[fontSizeKey] = clamped }
    }

    override suspend fun setFont(font: ReaderFont) {
        dataStore.edit { it[fontKey] = font.name }
    }

    override suspend fun setMonoFont(font: ReaderFont) {
        dataStore.edit { it[monoFontKey] = font.name }
    }

    override suspend fun setTheme(theme: ReaderTheme) {
        dataStore.edit { it[themeKey] = theme.name }
    }

    override suspend fun setJustify(justify: Boolean) {
        dataStore.edit { it[justifyKey] = justify }
    }

    override suspend fun setBgColor(color: Int?) {
        dataStore.edit { if (color == null) it.remove(bgColorKey) else it[bgColorKey] = color }
    }

    override suspend fun setTextColor(color: Int?) {
        dataStore.edit { if (color == null) it.remove(textColorKey) else it[textColorKey] = color }
    }

    override suspend fun setHighlightColor(color: Int) {
        dataStore.edit { it[highlightColorKey] = color }
    }
}
