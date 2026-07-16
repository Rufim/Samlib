package ru.samlib.client.data.local

import java.nio.charset.Charset
import kotlin.math.min

private val CP1251: Charset = Charset.forName("windows-1251")

/**
 * Определяет кодировку файла (для samlib реальны только cp1251 и utf-8).
 * HTML: ищем объявление `charset=` в первых 2048 байтах — оно лежит в `<head>`,
 * 2048 хватает с запасом, а полное сканирование многомегабайтного файла не нужно.
 * Иначе: пробуем utf-8; replacement character (�) в результате означает байты,
 * невалидные для utf-8, — значит это cp1251.
 */
fun decodeBytes(
    bytes: ByteArray,
    isHtml: Boolean,
): String {
    if (isHtml) {
        val head = String(bytes, 0, min(2048, bytes.size), Charsets.ISO_8859_1).lowercase()
        val idx = head.indexOf("charset=")
        if (idx >= 0) {
            val cs =
                head
                    .substring(idx + 8)
                    .trimStart('"', '\'', ' ')
                    .takeWhile { it.isLetterOrDigit() || it == '-' }
            if (cs.contains("1251")) return String(bytes, CP1251)
            if (cs.contains("utf")) return String(bytes, Charsets.UTF_8)
        }
    }
    val utf = String(bytes, Charsets.UTF_8)
    return if (utf.contains('�')) String(bytes, CP1251) else utf
}
