package ru.samlib.client.data.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import timber.log.Timber

/**
 * Аудиофокус для озвучки: пауза на звонок/чужую музыку, авто-возобновление после короткого
 * прерывания (transient loss), отказ от фокуса при явной остановке.
 */
class TtsAudioFocusController(
    context: Context,
    private val isPlaying: () -> Boolean,
    private val pause: () -> Unit,
    private val resume: () -> Unit,
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: AudioFocusRequest? = null
    private var pausedByFocusLoss = false

    private val focusListener =
        AudioManager.OnAudioFocusChangeListener { change ->
            when (change) {
                AudioManager.AUDIOFOCUS_LOSS -> {
                    Timber.tag(TAG).i("аудиофокус: AUDIOFOCUS_LOSS — пауза без автовозобновления")
                    pausedByFocusLoss = false
                    pause()
                }

                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK,
                -> {
                    Timber.tag(TAG).i(
                        "аудиофокус: временная потеря (%d), isPlaying=%b",
                        change,
                        isPlaying(),
                    )
                    if (isPlaying()) {
                        pausedByFocusLoss = true
                        pause()
                    }
                }

                AudioManager.AUDIOFOCUS_GAIN -> {
                    Timber.tag(TAG).i("аудиофокус: AUDIOFOCUS_GAIN, pausedByFocusLoss=%b", pausedByFocusLoss)
                    if (pausedByFocusLoss) {
                        pausedByFocusLoss = false
                        resume()
                    }
                }

                else -> Timber.tag(TAG).i("аудиофокус: неизвестное изменение=%d", change)
            }
        }

    fun request() {
        if (focusRequest != null) return
        val attrs =
            AudioAttributes
                .Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        val req =
            AudioFocusRequest
                .Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener(focusListener)
                .setWillPauseWhenDucked(true)
                .build()
        focusRequest = req
        val result = audioManager.requestAudioFocus(req)
        Timber.tag(TAG).i("аудиофокус запрошен: результат=%d", result)
    }

    fun abandon() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null
        pausedByFocusLoss = false
    }

    private companion object {
        const val TAG = "TtsPlayer"
    }
}
