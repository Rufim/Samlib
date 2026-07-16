package ru.samlib.client.data.parser

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ParserUtils {
    fun parseSizeKb(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        val digits = raw.trim().takeWhile { it.isDigit() }
        return digits.toIntOrNull()
    }

    fun parseDate(
        raw: String?,
        pattern: String = "dd/MM/yyyy",
    ): Date? {
        if (raw.isNullOrBlank()) return null
        return try {
            SimpleDateFormat(pattern, Locale.US).parse(raw.trim())
        } catch (e: Exception) {
            null
        }
    }
}
