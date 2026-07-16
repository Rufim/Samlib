package ru.samlib.client.data.tts

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack

/**
 * Беззвучный AudioTrack в нашем процессе на время озвучки. Физический звук TTS издаёт
 * процесс движка (например com.google.android.tts), поэтому AudioPlayerStateMonitor не
 * видит у нашего uid активного плеера и система отдаёт media-кнопки «последнему реально
 * игравшему» приложению. Тихий зацикленный трек (8 кГц mono, MODE_STATIC — без нагрузки
 * на CPU) делает наш uid играющим → media-кнопки маршрутизируются нашей сессии.
 */
class SilentAudioKeeper {
    private var track: AudioTrack? = null

    fun start() {
        if (track != null) return
        runCatching {
            val sampleRate = 8_000
            val minBuf =
                AudioTrack
                    .getMinBufferSize(
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                    ).coerceAtLeast(2_048)
            val t =
                AudioTrack(
                    AudioAttributes
                        .Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                    AudioFormat
                        .Builder()
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build(),
                    minBuf,
                    AudioTrack.MODE_STATIC,
                    AudioManager.AUDIO_SESSION_ID_GENERATE,
                )
            val silence = ByteArray(minBuf)
            t.write(silence, 0, silence.size)
            // цикл по всему буферу бесконечно; frame = 2 байта (16 бит mono)
            t.setLoopPoints(0, minBuf / 2, -1)
            t.play()
            track = t
        }.onFailure { track = null }
    }

    fun stop() {
        track?.let {
            runCatching {
                it.stop()
                it.release()
            }
        }
        track = null
    }
}
