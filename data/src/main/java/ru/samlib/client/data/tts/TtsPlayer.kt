package ru.samlib.client.data.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.samlib.client.domain.model.TtsPhrase
import ru.samlib.client.domain.repository.BlockTextExtractor
import ru.samlib.client.domain.repository.TtsEngineSettings
import ru.samlib.client.domain.repository.TtsEngineStatus
import ru.samlib.client.domain.repository.TtsPlaybackControls
import ru.samlib.client.domain.repository.TtsPlaybackState
import ru.samlib.client.domain.repository.TtsSettings
import ru.samlib.client.domain.repository.TtsStartRequest
import timber.log.Timber
import java.util.concurrent.atomic.AtomicLong

/**
 * Решает, когда сохранять прогресс чтения по TTS: только при смене блока и только
 * если известна ссылка книги. onBlock возвращает новое значение «последнего блока».
 */
class TtsProgressTracker(
    private val workLink: String?,
    private val onSave: (link: String, blockIndex: Int) -> Unit,
) {
    fun onBlock(
        blockIndex: Int,
        lastBlock: Int,
    ): Int {
        if (workLink != null && blockIndex != lastBlock) onSave(workLink, blockIndex)
        return blockIndex
    }
}

/**
 * Отличает колбэки движка от уже отменённой попытки озвучки (устаревшая пауза,
 * упреждающе добитая в очередь фраза до стопа) от актуальных. Каждый [bump] начинает
 * новую эпоху; [format]/[parse] переносят её в utteranceId движка.
 */
class TtsUtteranceEpoch {
    private var epoch = 0

    /** Начать новую попытку озвучки — колбэки предыдущей перестают быть актуальными. */
    fun bump() {
        epoch++
    }

    fun format(pos: Int): String = "u$epoch.$pos"

    /** null — колбэк устарел (другая эпоха) либо ID не распознан. */
    fun parse(id: String?): Int? {
        val raw = id?.removePrefix("u") ?: return null
        val dot = raw.indexOf('.')
        if (dot < 0) return null
        val utteranceEpoch = raw.substring(0, dot).toIntOrNull() ?: return null
        val pos = raw.substring(dot + 1).toIntOrNull() ?: return null
        return pos.takeIf { utteranceEpoch == epoch }
    }
}

/**
 * App-scoped владелец TextToSpeech: озвучивает список фраз (предложений) по очереди,
 * публикует состояние (StateFlow) для UI (панель/подсветка/скролл) и нотификации.
 * Живёт на уровне приложения → озвучка продолжается при свёрнутом экране/приложении.
 *
 * Сам сеанс TextToSpeech (создание/переключение движка, гонки асинхронного onInit) — в
 * [TtsEngineController]; аудиофокус — в [TtsAudioFocusController]; текст фраз — в [TtsTextCache].
 * Здесь — только оркестрация: очередь фраз, подсветка, реакция на события движка, таймер сна.
 */
class TtsPlayer(
    private val appContext: Context,
    private val textExtractor: BlockTextExtractor,
    // реактивно на каждый блок — пишет только в in-memory кеш репозитория (дёшево, надёжно)
    private val saveProgress: suspend (link: String, blockIndex: Int) -> Unit = { _, _ -> },
    // переносит кеш в БД — вызывается в контрольных точках (пауза/стоп/финиш), а не на каждый блок
    private val flushProgress: suspend (link: String) -> Unit = {},
) : TtsPlaybackControls,
    TtsEngineSettings {
    private val _state = MutableStateFlow(TtsPlaybackState())
    override val state: StateFlow<TtsPlaybackState> = _state.asStateFlow()

    // app-scope: прогресс и таймер сна работают даже при закрытом экране/в фоне.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val sleepTimer = SleepTimer(scope)
    private var sleepMinutes = 0
    private var progressTracker = TtsProgressTracker(null) { _, _ -> }
    private var lastSavedBlock = -1
    private var currentWorkLink: String? = null

    // Тихий трек: делает наш uid «играющим», чтобы система слала media-кнопки нашей сессии
    // (звук TTS физически издаёт процесс движка, не мы).
    private val silentKeeper = SilentAudioKeeper()

    // Аудиофокус: пауза на звонок/чужую музыку, авто-возобновление после короткого прерывания.
    private val audioFocus: TtsAudioFocusController =
        TtsAudioFocusController(
            appContext,
            isPlaying = { _state.value.playing },
            pause = { pause() },
            resume = { resume() },
        )

    // Мемо plain-текста текущего блока — не гоняем парсинг HTML на каждую фразу.
    private val textCache = TtsTextCache(textExtractor)

    private var pendingStart = false
    private var items: List<TtsPhrase> = emptyList()
    private var pos = 0
    private var rate = 1.0f
    private var pitch = 1.0f
    private var voiceName: String? = null

    // Билет из reserveStart() — растёт при каждом резерве. start() отбрасывает запрос, чей
    // билет устарел к моменту вызова: конкурирующий более поздний тап уже застолбил новый билет,
    // а фоновая разбивка на фразы для этого запроса могла завершиться позже него (см.
    // ReaderScaffold.startTts — тикет берётся синхронно ДО тяжёлой разбивки на фразы).
    private val startTicket = AtomicLong(0)

    // Дебаунс: гасит дребезг быстрых повторных start() — реально применяем только последний
    // вызов в пределах окна, не дёргая движок и не сбрасывая позицию на каждый промежуточный.
    private var startDebounceJob: Job? = null

    // id реплики несёт эпоху сессии озвучки — колбэк из отменённой попытки (напр. фраза,
    // упреждающе добитая в очередь до pause()) отсекается по несовпадению эпохи, а не по
    // текущему _state.playing.
    private val utteranceEpoch = TtsUtteranceEpoch()

    // Подсветку и подкачку следующей фразы делаем в onStart (по факту начала звучания):
    // подсвечиваем текущую (по utteranceId) и упреждающе ставим в очередь следующую
    // (QUEUE_ADD) — звук без разрыва, движок не забегает вперёд более чем на одну.
    // onDone лишь завершает на последней фразе. Крайние пробелы в подсветке не считаем.
    @Volatile
    private var queuedUpTo = -1

    // После простоя система убивает сервис TTS, а хендл остаётся не-null → speak() даёт ERROR.
    // Тогда пересоздаём движок (однократно до успешного onStart), чтобы восстановить озвучку.
    @Volatile
    private var recreatedAfterSpeakError = false

    private fun highlight(i: Int) {
        val p = items.getOrNull(i) ?: return
        pos = i
        lastSavedBlock = progressTracker.onBlock(p.blockIndex, lastSavedBlock)
        val text = textCache.phraseText(p)
        // крайние пробелы/переносы фразы не подсвечиваем — иначе выделение «уводит» на соседние строки
        val leading = text.length - text.trimStart().length
        val trailing = text.length - text.trimEnd().length
        val start = p.offset + leading
        val end = (p.offset + text.length - trailing).coerceAtLeast(start)
        _state.update { it.copy(blockIndex = p.blockIndex, wordStart = start, wordEnd = end) }
    }

    private val listener =
        object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                val i = utteranceEpoch.parse(utteranceId) ?: return
                // Речь реально пошла → движок жив, снимаем защиту от повторного пересоздания.
                recreatedAfterSpeakError = false
                highlight(i)
                // упреждающе ставим следующую фразу в очередь — воспроизведение без разрыва
                val next = i + 1
                if (next < items.size && next > queuedUpTo) {
                    engineController.engine?.speak(
                        "${textCache.phraseText(items[next])} ",
                        TextToSpeech.QUEUE_ADD,
                        null,
                        utteranceEpoch.format(next),
                    )
                    queuedUpTo = next
                }
            }

            override fun onRangeStart(
                utteranceId: String?,
                start: Int,
                end: Int,
                frame: Int,
            ) = Unit

            override fun onDone(utteranceId: String?) {
                val i =
                    utteranceEpoch.parse(utteranceId) ?: run {
                        Timber.tag(TAG).d("onDone: устаревший/нераспознанный utterance=%s — игнорирую", utteranceId)
                        return
                    }
                if (i >= items.size - 1 && _state.value.playing) finish()
            }

            /** Движок прервал реплику (не наш pause()/stop()) — сейчас единственный источник
             * данных о таких обрывах, до сих пор нигде не логировался. */
            override fun onStop(
                utteranceId: String?,
                interrupted: Boolean,
            ) {
                Timber.tag(TAG).i("onStop: utterance=%s, interrupted=%b", utteranceId, interrupted)
            }

            @Deprecated("deprecated in API")
            override fun onError(utteranceId: String?) = Unit

            override fun onError(
                utteranceId: String?,
                errorCode: Int,
            ) {
                Timber.tag(TAG).e("onError: utterance=%s, код движка=%d", utteranceId, errorCode)
            }
        }

    // Инициализация/переключение движка асинхронное — вся защита от гонок внутри контроллера
    // (см. его kdoc). onReady/onUnavailable замыкают его обратно на наше состояние pendingStart.
    private val engineController: TtsEngineController =
        TtsEngineController(
            appContext = appContext,
            listener = listener,
            onReady = {
                if (pendingStart) {
                    pendingStart = false
                    if (_state.value.playing) speakCurrent()
                }
            },
            onUnavailable = { pendingStart = false },
        )

    override val engineStatus: StateFlow<TtsEngineStatus> = engineController.engineStatus
    override val boundEngine: StateFlow<String?> = engineController.boundEngine

    override fun engines(): List<Pair<String, String>> = engineController.engines().map { it.name to it.label }

    override fun voices(language: String): List<String> = engineController.voices(language).map { it.name }

    override fun warmUp(engine: String?) = engineController.ensureEngine(engine)

    override fun reserveStart(): Long = startTicket.incrementAndGet()

    override fun start(
        request: TtsStartRequest,
        settings: TtsSettings,
    ) {
        if (request.ticket != startTicket.get()) {
            Timber.tag(TAG).i(
                "start: запрос устарел (билет=%d, актуальный=%d) — игнорирую",
                request.ticket,
                startTicket.get(),
            )
            return
        }
        // Мгновенная подсветка целевой позиции — раньше debounce, движка и его готовности,
        // чтобы пользователь сразу видел, откуда начнётся чтение, а не ждал engine/QUEUE_FLUSH.
        previewHighlight(request)
        startDebounceJob?.cancel()
        startDebounceJob =
            scope.launch {
                delay(START_DEBOUNCE_MS)
                performStart(request, settings)
            }
    }

    /** Позиция первой фразы со следующего блока либо содержащей/следующей за нажатым словом. */
    private fun computeStartPos(request: TtsStartRequest): Int =
        request.phrases
            .indexOfFirst {
                it.blockIndex > request.fromBlock ||
                    (it.blockIndex == request.fromBlock && it.offset + it.length > request.fromOffset)
            }.let { if (it < 0) 0 else it }

    /**
     * Синхронно, без ожидания движка: готовит данные фразы/прогресса и подсвечивает целевую
     * позицию. Если следом отменится debounce (ещё один тап раньше [START_DEBOUNCE_MS]) — это
     * не страшно, эту же работу повторно и корректно проделает более новый вызов.
     */
    private fun previewHighlight(request: TtsStartRequest) {
        items = request.phrases
        textCache.setBlocks(request.blocks)
        currentWorkLink = request.workLink
        progressTracker =
            TtsProgressTracker(request.workLink) { link, block ->
                scope.launch { runCatching { saveProgress(link, block) } }
            }
        lastSavedBlock = -1
        pos = computeStartPos(request)
        _state.value = TtsPlaybackState(active = true, playing = true, title = request.title)
        highlight(pos)
    }

    private fun performStart(
        request: TtsStartRequest,
        settings: TtsSettings,
    ) {
        engineController.ensureEngine(settings.engine)
        this.rate = settings.rate
        this.pitch = settings.pitch
        voiceName = settings.voice
        recreatedAfterSpeakError = false
        utteranceEpoch.bump()
        queuedUpTo = -1
        sleepMinutes = settings.sleepTimerMinutes
        Timber.tag(TAG).i(
            "start: title=%s, фраз=%d, fromBlock=%d, fromOffset=%d, engine=%s, voice=%s, rate=%.2f, pitch=%.2f",
            request.title,
            request.phrases.size,
            request.fromBlock,
            request.fromOffset,
            settings.engine,
            settings.voice,
            settings.rate,
            settings.pitch,
        )
        audioFocus.request()
        silentKeeper.start()
        if (engineController.ready) {
            speakCurrent()
        } else {
            pendingStart = true
            Timber.tag(TAG).i("start: движок ещё не готов — старт отложен (pendingStart)")
        }
        scheduleSleep()
    }

    /** Изменить скорость (во время drag): применится к следующей фразе; см. [applySpeechSettings]. */
    override fun setRate(value: Float) {
        rate = value
        resetSleep()
    }

    /** Изменить тембр (во время drag): применится к следующей фразе; см. [applySpeechSettings]. */
    override fun setPitch(value: Float) {
        pitch = value
        resetSleep()
    }

    /** Применить текущие скорость/тембр немедленно: прерываем и перечитываем текущую фразу. */
    override fun applySpeechSettings() {
        engineController.engine?.apply {
            setSpeechRate(rate)
            setPitch(pitch)
        }
    }

    override fun pause() {
        Timber.tag(TAG).i("pause")
        flushCurrent()
        utteranceEpoch.bump()
        queuedUpTo = -1
        _state.update { it.copy(playing = false) }
        engineController.engine?.stop()
        resetSleep()
        silentKeeper.stop()
    }

    /** Переносит закешированную позицию в БД — вызывается в контрольных точках жизненного цикла. */
    private fun flushCurrent() {
        val link = currentWorkLink ?: return
        scope.launch {
            runCatching { flushProgress(link) }
                .onFailure { Timber.tag(TAG).e(it, "flushCurrent: не удалось сохранить позицию link=%s", link) }
        }
    }

    /** Длительность таймера сна (минуты); при активной озвучке перепланирует отсчёт. */
    override fun setSleepTimer(minutes: Int) {
        sleepMinutes = minutes
        if (_state.value.active) scheduleSleep()
    }

    /** Сброс таймера сна на активности пользователя (напр. ручной скролл). */
    override fun resetSleepTimer() {
        if (_state.value.active) resetSleep()
    }

    private fun scheduleSleep() {
        sleepTimer.schedule(sleepMinutes) {
            // Отличаем от ручного/media-стопа в логах — иначе по "stop" в логе не понять,
            // истёк ли таймер сна или это нажатие пользователя/кнопка уведомления.
            Timber.tag(TAG).i("таймер сна истёк (%d мин) — останавливаю", sleepMinutes)
            stop()
        }
        updateSleepDeadline()
    }

    private fun resetSleep() {
        sleepTimer.reset()
        updateSleepDeadline()
    }

    /** Синхронизирует время остановки таймера сна в состоянии (для панели и медиа-уведомления). */
    private fun updateSleepDeadline() {
        val until =
            if (sleepMinutes > 0 && _state.value.active) {
                System.currentTimeMillis() + sleepMinutes * 60_000L
            } else {
                null
            }
        val cur = _state.value.sleepUntilMillis
        // Уведомление показывает время с точностью до минуты — не дёргаем состояние в пределах минуты.
        if (until == cur) return
        if (until != null && cur != null && until / 60_000L == cur / 60_000L) return
        _state.update { it.copy(sleepUntilMillis = until) }
    }

    /** Тап по играющему тексту — полная остановка: убиваем движок. Режим озвучки остаётся активным. */
    override fun stopPlayback() {
        flushCurrent()
        engineController.shutdown()
        pendingStart = false
        utteranceEpoch.bump()
        queuedUpTo = -1
        _state.update { it.copy(playing = false) }
    }

    override fun resume() {
        if (items.isEmpty()) {
            Timber.tag(TAG).w("resume: список фраз пуст — нечего возобновлять")
            return
        }
        audioFocus.request()
        silentKeeper.start()
        utteranceEpoch.bump()
        queuedUpTo = -1
        _state.update { it.copy(playing = true) }
        engineController.ensureEngine(engineController.currentEngine)
        if (engineController.ready) speakCurrent() else pendingStart = true
        resetSleep()
    }

    override fun stop() {
        Timber.tag(TAG).i("stop")
        flushCurrent()
        currentWorkLink = null
        utteranceEpoch.bump()
        queuedUpTo = -1
        _state.value = TtsPlaybackState()
        engineController.engine?.stop()
        sleepTimer.cancel()
        audioFocus.abandon()
        silentKeeper.stop()
        releaseContent()
    }

    /** Освобождаем память книги после полной остановки (плеер живёт на уровне приложения). */
    private fun releaseContent() {
        items = emptyList()
        textCache.clear()
    }

    /** Озвучить текущую фразу [pos] (QUEUE_FLUSH); следующую упреждающе поставит onStart. Подсветка — перед speak. */
    private fun speakCurrent() {
        val engine = engineController.engine
        if (engine == null) {
            Timber.tag(TAG).w("speakCurrent: движок не готов (tts==null) — фраза не озвучена")
            return
        }
        val p = items.getOrNull(pos)
        if (p == null) {
            Timber
                .tag(TAG)
                .i("speakCurrent: позиция %d вне списка (%d фраз) — завершение", pos, items.size)
            finish()
            return
        }
        engine.setSpeechRate(rate)
        engine.setPitch(pitch)
        voiceName?.let { name ->
            engine.voices?.firstOrNull { it.name == name }?.let { engine.voice = it }
        }
        highlight(pos)
        queuedUpTo = pos
        // Хвостовой пробел: многие движки обрезают последний звук фразы — паддинг спасает.
        val res =
            engine.speak(
                "${textCache.phraseText(p)} ",
                TextToSpeech.QUEUE_FLUSH,
                null,
                utteranceEpoch.format(pos),
            )
        if (res == TextToSpeech.ERROR) {
            if (recreatedAfterSpeakError) {
                Timber.tag(TAG).e(
                    "speakCurrent: speak() снова ERROR у u%d после пересоздания движка — сдаюсь",
                    pos,
                )
            } else {
                // Типичный случай: после простоя сервис движка мёртв. Пересоздаём и переиграем фразу.
                Timber.tag(TAG).w(
                    "speakCurrent: speak() ERROR у u%d — движок мёртв после простоя, пересоздаю",
                    pos,
                )
                recreatedAfterSpeakError = true
                pendingStart = true
                engineController.recreateCurrent()
            }
        }
    }

    private fun finish() {
        Timber.tag(TAG).i("finish: озвучка завершена (pos=%d, items=%d)", pos, items.size)
        flushCurrent()
        currentWorkLink = null
        utteranceEpoch.bump()
        queuedUpTo = -1
        _state.value = TtsPlaybackState()
        sleepTimer.cancel()
        audioFocus.abandon()
        silentKeeper.stop()
        releaseContent()
    }

    private companion object {
        const val TAG = "TtsPlayer"
        const val START_DEBOUNCE_MS = 100L
    }
}
