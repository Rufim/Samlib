package ru.samlib.client.data.log

import android.util.Log
import timber.log.Timber
import java.util.concurrent.Executors

/** Timber-дерево: форматирует запись и пишет её в файл на выделенном фоновом потоке. */
class FileLoggingTree(
    private val writer: LogFileWriter,
) : Timber.Tree() {
    private val io =
        Executors.newSingleThreadExecutor { r ->
            Thread(r, "file-log").apply { isDaemon = true }
        }

    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?,
    ) {
        val timeMillis = System.currentTimeMillis()
        io.execute {
            writer.append(FileLogFormatter.format(timeMillis, priority, tag, message))
            if (t != null) writer.append(Log.getStackTraceString(t) + "\n")
        }
    }
}
