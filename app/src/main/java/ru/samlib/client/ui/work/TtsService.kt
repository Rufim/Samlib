package ru.samlib.client.ui.work

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import ru.samlib.client.MainActivity
import ru.samlib.client.data.tts.TtsPlayer
import ru.samlib.client.ui.R
import timber.log.Timber

/**
 * media3 [MediaSessionService]: уведомление и foreground ведёт media3
 * (DefaultMediaNotificationProvider). Чтобы пайплайн уведомления media3 запустился, сервис
 * сам подключает media3 [MediaController] к своей сессии. Кнопка «Стоп» — кастомная команда
 * (setCustomLayout), видна в медиа-карточке. media-кнопки/локскрин — тоже через сессию.
 */
@OptIn(UnstableApi::class)
class TtsService : MediaSessionService() {
    private val player: TtsPlayer by inject()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var session: MediaSession? = null
    private var sessionPlayer: TtsSessionPlayer? = null
    private var controller: MediaController? = null
    private var noisyRegistered = false

    // Диагностика внезапных обрывов фонового воспроизведения: отличаем наш собственный
    // stopSelf() (когда TtsPlayer.state.active стал false) от уничтожения сервиса системой
    // (task killer/Doze/OOM) — во втором случае onDestroy придёт без этого флага.
    private var selfStopRequested = false

    private val stopButton: CommandButton by lazy {
        CommandButton
            .Builder(CommandButton.ICON_STOP)
            .setDisplayName(getString(R.string.reader_tts_stop_button))
            .setSessionCommand(SessionCommand(CUSTOM_STOP, Bundle.EMPTY))
            .build()
    }

    /** Отключение наушников (BECOMING_NOISY) → пауза, чтобы не «орало» из динамика. */
    private val noisyReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) player.pause()
            }
        }

    override fun onCreate() {
        super.onCreate()
        Timber.tag(TAG).i("onCreate")
        val sp =
            TtsSessionPlayer(
                player,
                mainLooper,
                scope,
                getString(R.string.reader_tts),
                getString(R.string.reader_tts_sleep_until),
            )
        sessionPlayer = sp
        session =
            MediaSession
                .Builder(this, sp)
                .setSessionActivity(openAppIntent())
                .setCallback(SessionCallback())
                .setCustomLayout(listOf(stopButton))
                .build()
        registerReceiver(noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
        noisyRegistered = true
        connectController()
        // Гасим сервис, когда озвучка полностью остановлена.
        scope.launch {
            player.state.collect {
                if (!it.active) {
                    Timber.tag(TAG).i("state.collect: active=false → stopSelf()")
                    selfStopRequested = true
                    stopSelf()
                }
            }
        }
    }

    /** Подключаем media3-контроллер к своей же сессии — это запускает media3-уведомление. */
    private fun connectController() {
        val token = SessionToken(this, ComponentName(this, TtsService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        future.addListener(
            {
                controller = runCatching { future.get() }.getOrNull()
                if (controller == null) {
                    Timber.tag(TAG).w("connectController: MediaController не подключился — уведомления не будет")
                } else {
                    Timber.tag(TAG).i("connectController: MediaController подключен")
                }
                sessionPlayer?.prime()
            },
            ContextCompat.getMainExecutor(this),
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    /** Не переопределяем поведение — только логируем, чтобы отличить снятие задачи из
     * недавних от прочих причин уничтожения сервиса. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        Timber.tag(TAG).i("onTaskRemoved: playing=%b", player.state.value.playing)
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        Timber.tag(TAG).i("onDestroy: selfInitiated=%b", selfStopRequested)
        if (noisyRegistered) {
            runCatching { unregisterReceiver(noisyReceiver) }
            noisyRegistered = false
        }
        controller?.let { runCatching { it.release() } }
        controller = null
        session?.release()
        session = null
        sessionPlayer = null
        scope.cancel()
        super.onDestroy()
    }

    private inner class SessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val commands =
                MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                    .buildUpon()
                    .add(SessionCommand(CUSTOM_STOP, Bundle.EMPTY))
                    .build()
            return MediaSession.ConnectionResult
                .AcceptedResultBuilder(session)
                .setAvailableSessionCommands(commands)
                .build()
        }

        override fun onPostConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ) {
            super.onPostConnect(session, controller)
            session.setCustomLayout(listOf(stopButton))
            sessionPlayer?.prime()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == CUSTOM_STOP) {
                player.stop()
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
        }
    }

    /** Клик по уведомлению открывает приложение (возврат к текущей книге). */
    private fun openAppIntent(): PendingIntent {
        val intent =
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val TAG = "TtsService"
        private const val CUSTOM_STOP = "ru.samlib.client.tts.CUSTOM_STOP"

        fun start(context: Context) {
            val intent = Intent(context, TtsService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
