package ru.samlib.client.data.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.samlib.client.domain.repository.TtsEngineStatus
import timber.log.Timber
import java.util.Locale

/**
 * Владеет живым экземпляром [TextToSpeech] и его (пере)созданием при смене движка.
 * Инициализация асинхронная (`TextToSpeech.OnInitListener`), поэтому здесь же вся защита
 * от гонок, связанных с этим:
 * - [engineGeneration] — колбэк устаревшей (уже отменённой новым вызовом) попытки инициализации
 *   молча игнорируется; иначе он перезапишет состояние уже переключённого движка чужими данными
 *   (движок инициализируется второй раз до того, как отработал колбэк первого — например,
 *   пользователь быстро потыкал чипы движков в настройках).
 * - [boundEngine] — движок, который РЕАЛЬНО сейчас забинден, в отличие от просто запрошенного:
 *   обновляется только по факту успешного `onInit`. Настройки должны сверяться с этим полем,
 *   а не с тем, что выбрал пользователь: переключение асинхронное, и есть кадр, где статус уже
 *   READY, но относится к старому движку — иначе список голосов на миг покажет голоса не того
 *   движка, что выбран.
 *
 * [onReady] дёргается по факту успешной инициализации — вызывающий сам решает, нужно ли
 * (во)зобновить озвучку. [onUnavailable] — когда TTS на устройстве нет вообще ни одного движка.
 */
class TtsEngineController(
    private val appContext: Context,
    private val listener: UtteranceProgressListener,
    private val onReady: () -> Unit,
    private val onUnavailable: () -> Unit,
) {
    private val _engineStatus = MutableStateFlow(TtsEngineStatus.LOADING)
    val engineStatus: StateFlow<TtsEngineStatus> = _engineStatus.asStateFlow()

    private val _boundEngine = MutableStateFlow<String?>(null)
    val boundEngine: StateFlow<String?> = _boundEngine.asStateFlow()

    var engine: TextToSpeech? = null
        private set
    var ready: Boolean = false
        private set
    var currentEngine: String? = null
        private set

    // Защита от петли отката: пробуем дефолтный движок при неудаче конкретного лишь однажды.
    private var triedDefaultFallback = false
    private var engineGeneration = 0

    fun engines(): List<TextToSpeech.EngineInfo> = engine?.engines ?: emptyList()

    /** Голоса движка для языка [languageTag] (BCP-47/ISO2, напр. «ru»/«en»); по умолчанию — русский. */
    fun voices(languageTag: String = "ru"): List<Voice> =
        runCatching {
            engine?.voices?.filter { voiceMatchesLanguage(it.locale, languageTag) }?.sortedBy { it.name }
                ?: emptyList()
        }.getOrDefault(emptyList())

    /** Прогрев/переключение движка — не пересоздаёт, если [requested] уже забинден. */
    fun ensureEngine(requested: String?) {
        if (engine != null && requested == currentEngine) return
        createEngine(requested)
    }

    /** Принудительно пересоздать ТЕКУЩИЙ движок (напр. после ERROR от speak() — сервис умер). */
    fun recreateCurrent() = createEngine(currentEngine)

    private fun createEngine(requested: String?) {
        engine?.shutdown()
        engine = null
        ready = false
        _engineStatus.value = TtsEngineStatus.LOADING
        currentEngine = requested
        val myGeneration = ++engineGeneration
        val cb =
            TextToSpeech.OnInitListener { status ->
                if (myGeneration != engineGeneration) {
                    // Эту попытку уже отменили новым createEngine() (напр. пользователь успел
                    // переключить движок ещё раз) — колбэк опоздал, применять его нельзя: он
                    // читает/пишет engine/currentEngine как общие поля и перезапишет актуальный
                    // движок чужими данными.
                    Timber.tag(TAG).i(
                        "createEngine: устаревший колбэк движка '%s' (поколение %d, актуально %d) — игнорирую",
                        requested,
                        myGeneration,
                        engineGeneration,
                    )
                    return@OnInitListener
                }
                if (status == TextToSpeech.SUCCESS) {
                    triedDefaultFallback = false
                    ready = true
                    // На устройстве/профиле может не быть ни одного установленного движка —
                    // тогда списки пусты и в настройках нужно показать уведомление, а не пустоту.
                    val hasEngines = runCatching { engine?.engines?.isNotEmpty() }.getOrNull() == true
                    _engineStatus.value =
                        if (hasEngines) TtsEngineStatus.READY else TtsEngineStatus.UNAVAILABLE
                    // Выставляем ПОСЛЕ engineStatus: коллекторы (Settings) реагируют на статус и
                    // читают boundEngine синхронно следом — порядок эмиссий тут не даёт разницы,
                    // но boundEngine обязан быть готов к моменту READY.
                    _boundEngine.value = currentEngine
                    Timber.tag(TAG).i(
                        "движок инициализирован: engine=%s, hasEngines=%b",
                        currentEngine,
                        hasEngines,
                    )
                    val langRes = engine?.setLanguage(Locale("ru"))
                    if (langRes == TextToSpeech.LANG_MISSING_DATA || langRes == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Timber.tag(TAG).w("русский язык не поддержан движком: код=%d", langRes)
                    }
                    engine?.setOnUtteranceProgressListener(listener)
                    onReady()
                } else if (!requested.isNullOrBlank() && !triedDefaultFallback) {
                    // Выбранный движок не запустился (нет в профиле) — один раз откатываемся на
                    // системный по умолчанию, чтобы пользователь не остался без озвучки.
                    Timber
                        .tag(TAG)
                        .w("движок '%s' не запустился — откат на системный по умолчанию", requested)
                    triedDefaultFallback = true
                    createEngine(null)
                } else {
                    // Даже дефолтный движок не поднялся — TTS на устройстве нет.
                    ready = false
                    _engineStatus.value = TtsEngineStatus.UNAVAILABLE
                    _boundEngine.value = null
                    onUnavailable()
                    Timber.tag(TAG).e("TTS недоступен: ни один движок не инициализирован")
                }
            }
        engine =
            if (requested.isNullOrBlank()) {
                TextToSpeech(appContext, cb)
            } else {
                TextToSpeech(appContext, cb, requested)
            }
    }

    /** Полная остановка: убивает движок (напр. тап по играющему тексту). */
    fun shutdown() {
        engine?.stop()
        engine?.shutdown()
        engine = null
        ready = false
    }

    private companion object {
        const val TAG = "TtsPlayer"
    }
}
