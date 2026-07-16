package ru.samlib.client.data.log

import android.content.Context
import android.util.Log
import timber.log.Timber

/** Единая точка управления логированием: logcat всегда, файл — по флагу, краши — всегда в файл. */
object LogManager {
    private var writer: LogFileWriter? = null
    private var fileTree: FileLoggingTree? = null

    /** Готовит writer и сажает logcat-дерево. Идемпотентно. */
    fun init(context: Context) {
        if (writer != null) return
        writer = LogFileWriter(context.applicationContext)
        Timber.plant(Timber.DebugTree())
    }

    /** Включает/выключает запись в файл в рантайме (идемпотентно). */
    @Synchronized
    fun setFileLogging(enabled: Boolean) {
        val w = writer ?: return
        if (enabled && fileTree == null) {
            fileTree = FileLoggingTree(w).also { Timber.plant(it) }
        } else if (!enabled) {
            fileTree?.let { Timber.uproot(it) }
            fileTree = null
        }
    }

    /**
     * Пишет необработанные исключения в файл синхронно и безусловно (даже при выключенном
     * файловом логе), затем делегирует прежнему обработчику, чтобы не сломать штатное поведение.
     */
    fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                writer?.let { w ->
                    w.append(
                        FileLogFormatter.format(
                            System.currentTimeMillis(),
                            Log.ASSERT,
                            "CRASH",
                            "Необработанное исключение в потоке ${thread.name}",
                        ),
                    )
                    w.append(Log.getStackTraceString(throwable) + "\n")
                }
            }
            previous?.uncaughtException(thread, throwable)
        }
    }
}
