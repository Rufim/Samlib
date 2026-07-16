package ru.samlib.client.data.tts

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Таймер бездействия для озвучки. schedule запускает отсчёт на minutes минут;
 * reset перезапускает его с той же длительностью; cancel отменяет.
 * minutes <= 0 — таймер выключен. Работает на переданном scope (у TtsPlayer — app-scope).
 */
class SleepTimer(
    private val scope: CoroutineScope,
) {
    private var job: Job? = null
    private var minutes: Int = 0
    private var onElapse: (() -> Unit)? = null

    fun schedule(
        minutes: Int,
        onElapse: () -> Unit,
    ) {
        this.minutes = minutes
        this.onElapse = onElapse
        restart()
    }

    fun reset() = restart()

    fun cancel() {
        job?.cancel()
        job = null
    }

    private fun restart() {
        job?.cancel()
        val m = minutes
        val cb = onElapse
        if (m <= 0 || cb == null) {
            job = null
            return
        }
        job =
            scope.launch {
                delay(m.toLong() * 60_000L)
                cb()
            }
    }
}
