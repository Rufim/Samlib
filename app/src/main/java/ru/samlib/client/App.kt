package ru.samlib.client

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import ru.samlib.client.data.log.LogManager
import ru.samlib.client.di.appModule
import ru.samlib.client.di.dataModules
import ru.samlib.client.di.uiModule
import ru.samlib.client.domain.repository.ReaderSettingsRepository
import ru.samlib.client.work.FollowedUpdateWorker
import timber.log.Timber

class App : Application() {
    // app-scope: подписка на флаг файлового лога живёт всё время работы приложения.
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        // до Koin/воркеров — чтобы ловить ранние краши и логи старта.
        LogManager.init(this)
        LogManager.installCrashHandler()
        val koin =
            startKoin {
                androidLogger()
                androidContext(this@App)
                modules(listOf(appModule) + dataModules + uiModule)
            }.koin
        val settingsRepo = koin.get<ReaderSettingsRepository>()
        appScope.launch {
            settingsRepo.fileLogging.collect { LogManager.setFileLogging(it) }
        }
        FollowedUpdateWorker.schedule(this)
    }

    // Сигналы давления памяти нередко предшествуют убийству фонового процесса системой/OEM
    // (TTS обрывается без единого нашего лога) — фиксируем их, чтобы сопоставить по времени.
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Timber.tag(TAG).w("onTrimMemory: level=%d", level)
    }

    @Deprecated("Deprecated in API 34, оставлено для устройств ниже")
    override fun onLowMemory() {
        super.onLowMemory()
        Timber.tag(TAG).w("onLowMemory")
    }

    private companion object {
        const val TAG = "App"
    }
}
