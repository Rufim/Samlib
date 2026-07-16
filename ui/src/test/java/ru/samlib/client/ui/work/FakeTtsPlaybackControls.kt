package ru.samlib.client.ui.work

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import ru.samlib.client.domain.repository.TtsPlaybackControls
import ru.samlib.client.domain.repository.TtsPlaybackState
import ru.samlib.client.domain.repository.TtsSettings
import ru.samlib.client.domain.repository.TtsStartRequest

/** Тестовый двойник TtsPlaybackControls — ViewModel-тестам нужен только факт вызова start(). */
class FakeTtsPlaybackControls : TtsPlaybackControls {
    private val _state = MutableStateFlow(TtsPlaybackState())
    override val state: StateFlow<TtsPlaybackState> = _state

    var lastStartRequest: TtsStartRequest? = null
        private set
    private var ticket = 0L

    override fun reserveStart(): Long = ++ticket

    override fun start(
        request: TtsStartRequest,
        settings: TtsSettings,
    ) {
        lastStartRequest = request
    }

    override fun resume() = Unit

    override fun pause() = Unit

    override fun stop() = Unit

    override fun setSleepTimer(minutes: Int) = Unit

    override fun resetSleepTimer() = Unit

    override fun stopPlayback() = Unit

    override fun setRate(value: Float) = Unit

    override fun setPitch(value: Float) = Unit

    override fun applySpeechSettings() = Unit
}
