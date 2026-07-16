package ru.samlib.client.ui.work

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import ru.samlib.client.domain.repository.LegacyImportReport
import ru.samlib.client.domain.repository.ReaderSettingsRepository
import ru.samlib.client.domain.repository.Translator
import ru.samlib.client.domain.repository.TtsEngineSettings
import ru.samlib.client.domain.repository.TtsEngineStatus
import ru.samlib.client.domain.repository.WorkRepository
import ru.samlib.client.domain.settings.ReaderFont
import ru.samlib.client.domain.settings.ReaderTheme
import ru.samlib.client.domain.usecase.ImportLegacyDataUseCase
import java.io.File

@Immutable
data class SettingsState(
    val settings: UiReaderSettings = UiReaderSettings(),
    val offlineForced: Boolean = false,
    val fileLogging: Boolean = false,
    val domain: String = "",
    val proxy: String = "",
    val ttsStatus: TtsEngineStatus = TtsEngineStatus.LOADING,
    /** Движок, который РЕАЛЬНО сейчас забинден в TtsPlayer (см. [TtsEngineSettings.boundEngine]). */
    val boundEngine: String? = null,
)

sealed interface SettingsSideEffect {
    data object CacheCleared : SettingsSideEffect

    data class LegacyImported(
        val report: LegacyImportReport,
    ) : SettingsSideEffect

    data object LegacyNotFound : SettingsSideEffect
}

class SettingsViewModel(
    private val settingsRepo: ReaderSettingsRepository,
    private val workRepo: WorkRepository,
    private val translator: Translator,
    private val ttsPlayer: TtsEngineSettings,
    private val importLegacyData: ImportLegacyDataUseCase,
) : ViewModel(),
    ContainerHost<SettingsState, SettingsSideEffect> {
    override val container =
        viewModelScope.container<SettingsState, SettingsSideEffect>(SettingsState())

    /** Языки перевода ML Kit (BCP-47) для выбора направления в настройках. */
    fun translateLanguages(): List<String> = translator.availableLanguages()

    /** Движки TTS, доступные на устройстве (имя, отображаемая метка). */
    fun availableTtsEngines(): List<Pair<String, String>> = ttsPlayer.engines()

    /** Голоса выбранного движка TTS, поддерживающие язык [language] (BCP-47) — см. [UiReaderSettings.voiceLanguage]. */
    fun availableTtsVoices(language: String): List<String> = ttsPlayer.voices(language)

    fun clearCache() =
        intent {
            workRepo.clearWorkCache()
            postSideEffect(SettingsSideEffect.CacheCleared)
        }

    /** Перенос из legacy-БД, найденной на устройстве (обновление поверх). */
    fun importLegacyDefault() =
        intent {
            when (val result = importLegacyData(null)) {
                ImportLegacyDataUseCase.Result.NotFound -> postSideEffect(SettingsSideEffect.LegacyNotFound)
                is ImportLegacyDataUseCase.Result.Imported ->
                    postSideEffect(SettingsSideEffect.LegacyImported(result.report))
            }
        }

    /** Перенос из выбранного вручную файла БД (уже скопированного в [path]). */
    fun importLegacyFile(path: String) =
        intent {
            when (val result = importLegacyData(File(path))) {
                // file всегда передан явно — сюда не попадаем, ветка нужна для exhaustive when
                ImportLegacyDataUseCase.Result.NotFound -> Unit
                is ImportLegacyDataUseCase.Result.Imported ->
                    postSideEffect(SettingsSideEffect.LegacyImported(result.report))
            }
        }

    fun observeSettings() =
        intent {
            settingsRepo.settings.collect { s -> reduce { state.copy(settings = s.toUi()) } }
        }

    fun observeOffline() =
        intent {
            settingsRepo.offlineForced.collect { v -> reduce { state.copy(offlineForced = v) } }
        }

    fun setOfflineForced(value: Boolean) = intent { settingsRepo.setOfflineForced(value) }

    fun observeFileLogging() =
        intent {
            settingsRepo.fileLogging.collect { v -> reduce { state.copy(fileLogging = v) } }
        }

    fun setFileLogging(value: Boolean) = intent { settingsRepo.setFileLogging(value) }

    fun observeNetwork() =
        intent {
            settingsRepo.network.collect { n ->
                reduce {
                    state.copy(
                        domain = n.domain,
                        proxy = n.proxy,
                    )
                }
            }
        }

    fun setSamlibDomain(domain: String) = intent { settingsRepo.setSamlibDomain(domain) }

    fun setProxy(proxy: String) = intent { settingsRepo.setProxy(proxy) }

    fun setFontSize(size: Int) = intent { settingsRepo.setFontSize(size) }

    fun setFont(font: ReaderFont) = intent { settingsRepo.setFont(font) }

    fun setMonoFont(font: ReaderFont) = intent { settingsRepo.setMonoFont(font) }

    fun setTheme(theme: ReaderTheme) = intent { settingsRepo.setTheme(theme) }

    fun setTranslateTarget(lang: String) = intent { settingsRepo.setTranslateTarget(lang) }

    /** null — вернуться к автоопределению языка-источника перевода через ML Kit. */
    fun setTranslateSource(lang: String?) = intent { settingsRepo.setTranslateSource(lang) }

    fun setJustify(justify: Boolean) = intent { settingsRepo.setJustify(justify) }

    fun setBgColor(color: Int?) = intent { settingsRepo.setBgColor(color) }

    fun setTextColor(color: Int?) = intent { settingsRepo.setTextColor(color) }

    fun setHighlightColor(color: Int) = intent { settingsRepo.setHighlightColor(color) }

    fun setAutoScrollSpeed(speed: Int) = intent { settingsRepo.setAutoScrollSpeed(speed) }

    fun setTtsRate(rate: Float) = intent { settingsRepo.setTtsRate(rate) }

    fun setTtsPitch(pitch: Float) = intent { settingsRepo.setTtsPitch(pitch) }

    // Голос — сбрасываем на «Авто»: голос, выбранный вручную у предыдущего движка, может не
    // существовать в новом (имена голосов у движков не пересекаются), а без сброса такой
    // «осиротевший» выбор просто не найдёт соответствия в TtsPlayer.voices() — озвучка тихо
    // продолжит идти дефолтным голосом движка, при этом UI будет показывать несуществующий выбор.
    fun setTtsEngine(engine: String?) =
        intent {
            settingsRepo.setTtsEngine(engine)
            settingsRepo.setTtsVoice(null)
        }

    fun setTtsVoice(voice: String?) = intent { settingsRepo.setTtsVoice(voice) }

    /** Язык, которым фильтруется список голосов (см. [UiReaderSettings.voiceLanguage]). */
    fun setVoiceLanguage(lang: String) = intent { settingsRepo.setVoiceLanguage(lang) }

    /** Сохраняет таймер сна и сразу применяет его к активной озвучке. */
    fun setSleepTimer(minutes: Int) =
        intent {
            settingsRepo.setSleepTimer(minutes)
            ttsPlayer.setSleepTimer(minutes)
        }

    fun observeTtsStatus() =
        intent {
            ttsPlayer.engineStatus.collect { s -> reduce { state.copy(ttsStatus = s) } }
        }

    /** Движок, реально забинденный в TtsPlayer — см. [SettingsState.boundEngine]. */
    fun observeBoundEngine() =
        intent {
            ttsPlayer.boundEngine.collect { e -> reduce { state.copy(boundEngine = e) } }
        }

    /** Прогрев выбранного TTS-движка, чтобы списки движков/голосов появились в настройках. */
    fun warmUpTts(engine: String?) = intent { ttsPlayer.warmUp(engine) }
}
