package ru.samlib.client.data.log

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Форматирование строк лога и решение о ротации — чистые функции без IO. */
object FileLogFormatter {
    const val MAX_BYTES: Long = 2L * 1024 * 1024

    private fun priorityLetter(priority: Int): Char =
        when (priority) {
            Log.VERBOSE -> 'V'
            Log.DEBUG -> 'D'
            Log.INFO -> 'I'
            Log.WARN -> 'W'
            Log.ERROR -> 'E'
            Log.ASSERT -> 'A'
            else -> '?'
        }

    /** Строка вида "2026-07-10 12:00:00.123 D/TtsPlayer: сообщение\n". */
    fun format(
        timeMillis: Long,
        priority: Int,
        tag: String?,
        message: String,
    ): String {
        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ROOT).format(Date(timeMillis))
        val t = tag ?: "App"
        return "$ts ${priorityLetter(priority)}/$t: $message\n"
    }

    /** Нужно ли обнулить файл перед дозаписью, чтобы не превысить лимит. */
    fun shouldRotate(
        currentSizeBytes: Long,
        incomingBytes: Int,
        maxBytes: Long = MAX_BYTES,
    ): Boolean = currentSizeBytes + incomingBytes > maxBytes
}
