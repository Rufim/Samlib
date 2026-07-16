package ru.samlib.client.ui.work

import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ru.samlib.client.domain.repository.TtsControls
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Адаптер TTS под media3 Player для MediaSession/уведомления.
 * Прайминг: пока [prime] не вызван, плеер сообщает STATE_IDLE/не играет; затем — реальное
 * состояние. Это даёт переход «не играет → играет» уже после подписки нотификейшн-менеджера
 * media3, иначе уведомление не постится.
 */
@OptIn(UnstableApi::class)
class TtsSessionPlayer(
    private val controls: TtsControls,
    looper: Looper,
    scope: CoroutineScope,
    private val defaultTitle: String,
    // Формат подписи таймера сна для уведомления, напр. "Таймер сна до %1$s".
    private val sleepLabel: String,
) : SimpleBasePlayer(looper) {
    private var primed = false

    init {
        scope.launch {
            controls.state.collect { invalidateState() }
        }
    }

    /** Разрешить отражение реального состояния (после подключения media3-контроллера). */
    fun prime() {
        if (primed) return
        primed = true
        invalidateState()
    }

    override fun getState(): State {
        val s = controls.state.value
        val active = primed && s.active
        val playing = primed && s.playing
        val commands =
            Player.Commands
                .Builder()
                .addAll(
                    Player.COMMAND_PLAY_PAUSE,
                    Player.COMMAND_STOP,
                    Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                    Player.COMMAND_GET_METADATA,
                ).build()
        val meta = MediaMetadata.Builder().setTitle(s.title ?: defaultTitle)
        s.sleepUntilMillis?.let { until ->
            val hhmm = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(until))
            meta.setArtist(sleepLabel.format(hhmm))
        }
        val item =
            MediaItemData
                .Builder("tts")
                .setMediaItem(
                    MediaItem
                        .Builder()
                        .setMediaId("tts")
                        .setMediaMetadata(meta.build())
                        .build(),
                ).build()
        return State
            .Builder()
            .setAvailableCommands(commands)
            .setPlaybackState(if (active) Player.STATE_READY else Player.STATE_IDLE)
            .setPlayWhenReady(playing, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            // media-атрибуты: платформа считает сессию медиа-сессией и шлёт ей media-кнопки
            .setAudioAttributes(
                AudioAttributes
                    .Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
            ).setPlaylist(listOf(item))
            .build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        if (playWhenReady) controls.resume() else controls.pause()
        return Futures.immediateVoidFuture()
    }

    override fun handleStop(): ListenableFuture<*> {
        controls.stop()
        return Futures.immediateVoidFuture()
    }
}
