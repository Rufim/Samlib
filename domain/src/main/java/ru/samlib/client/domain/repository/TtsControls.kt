package ru.samlib.client.domain.repository

import kotlinx.coroutines.flow.StateFlow
import ru.samlib.client.domain.model.ContentBlock
import ru.samlib.client.domain.model.TtsPhrase

data class TtsPlaybackState(
    val active: Boolean = false,
    val playing: Boolean = false,
    val blockIndex: Int = -1,
    val wordStart: Int = -1,
    val wordEnd: Int = -1,
    val title: String? = null,
    /** Время (epoch ms), когда таймер сна остановит озвучку; null — таймер выключен. */
    val sleepUntilMillis: Long? = null,
)

/** Абстракция управления озвучкой для media-сессии (развязка от TextToSpeech). */
interface TtsControls {
    val state: StateFlow<TtsPlaybackState>

    fun resume()

    fun pause()

    fun stop()
}

/** Что и откуда озвучивать при старте TTS. */
data class TtsStartRequest(
    val title: String?, // заголовок для уведомления
    val phrases: List<TtsPhrase>, // разбитые на фразы блоки
    val blocks: List<ContentBlock>, // исходные блоки (для прогресса/сохранения)
    val fromBlock: Int, // с какого блока начать
    val fromOffset: Int = 0, // смещение внутри блока (символы)
    val workLink: String? = null, // ссылка произведения для сохранения позиции
    // Билет из reserveStart(), взятый ДО тяжёлой фоновой разбивки на фразы. Если к моменту
    // готовности этого запроса кто-то успел зарезервировать билет новее — start() его игнорирует
    // (см. kdoc reserveStart): защита от гонки конкурирующих вызовов start() при повторных тапах.
    val ticket: Long,
)

/** Параметры движка и озвучки TTS. */
data class TtsSettings(
    val rate: Float, // скорость речи
    val pitch: Float, // высота голоса
    val voice: String?, // имя голоса (null — движок по умолчанию)
    val engine: String?, // пакет TTS-движка
    val sleepTimerMinutes: Int = 0, // таймер сна, 0 — выключен
)

/**
 * Полный набор команд управления озвучкой книги — шире [TtsControls] (там только то, что
 * нужно медиа-сессии). Нужен читалке (`ReaderScaffold` в `:ui`), которая напрямую дирижирует
 * запуском/паузой/скоростью, а не только play/pause/stop.
 */
interface TtsPlaybackControls : TtsControls {
    /**
     * Застолбить билет для нового старта — вызывать синхронно ДО тяжёлой фоновой разбивки
     * на фразы (см. [TtsStartRequest.ticket]). [start] с устаревшим билетом будет проигнорирован.
     */
    fun reserveStart(): Long

    fun start(
        request: TtsStartRequest,
        settings: TtsSettings,
    )

    fun setSleepTimer(minutes: Int)

    fun resetSleepTimer()

    fun stopPlayback()

    fun setRate(value: Float)

    fun setPitch(value: Float)

    fun applySpeechSettings()
}
