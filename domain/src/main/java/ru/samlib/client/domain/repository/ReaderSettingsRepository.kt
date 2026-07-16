package ru.samlib.client.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.samlib.client.domain.settings.NetworkSettings
import ru.samlib.client.domain.settings.ReaderFont
import ru.samlib.client.domain.settings.ReaderSettings
import ru.samlib.client.domain.settings.ReaderTheme

interface ReaderSettingsRepository {
    /** Был ли уже выполнен перенос из legacy-БД. */
    suspend fun legacyImportDoneOnce(): Boolean

    suspend fun setLegacyImportDone()

    /** Последняя открытая книга (или null) — прочитать один раз при старте. */
    suspend fun lastBookOnce(): String?

    suspend fun setLastBook(value: String)

    suspend fun clearLastBook()

    /** Принудительный офлайн-режим (переключатель в настройках). */
    val offlineForced: Flow<Boolean>

    suspend fun setOfflineForced(value: Boolean)

    /** Запись лога приложения в файл в Download (переключатель в настройках). */
    val fileLogging: Flow<Boolean>

    suspend fun setFileLogging(value: Boolean)

    /** Домен samlib (пусто = по умолчанию http://samlib.ru) и HTTP-прокси «host:port». */
    val network: Flow<NetworkSettings>

    suspend fun setSamlibDomain(domain: String)

    suspend fun setProxy(proxy: String)

    val settings: Flow<ReaderSettings>

    suspend fun setSleepTimer(minutes: Int)

    suspend fun setAutoScrollSpeed(speed: Int)

    suspend fun setTtsRate(rate: Float)

    suspend fun setTtsPitch(pitch: Float)

    suspend fun setTtsEngine(engine: String?)

    suspend fun setTtsVoice(voice: String?)

    suspend fun setVoiceLanguage(lang: String)

    suspend fun setTranslateTarget(lang: String)

    suspend fun setTranslateSource(lang: String?)

    suspend fun setFontSize(size: Int)

    suspend fun setFont(font: ReaderFont)

    suspend fun setMonoFont(font: ReaderFont)

    suspend fun setTheme(theme: ReaderTheme)

    suspend fun setJustify(justify: Boolean)

    suspend fun setBgColor(color: Int?)

    suspend fun setTextColor(color: Int?)

    suspend fun setHighlightColor(color: Int)
}
