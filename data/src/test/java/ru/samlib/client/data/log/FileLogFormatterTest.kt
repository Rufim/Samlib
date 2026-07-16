package ru.samlib.client.data.log

import android.util.Log
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileLogFormatterTest {
    @Test
    fun `format строит строку с датой уровнем тегом и переводом строки`() {
        val line = FileLogFormatter.format(0L, Log.DEBUG, "TtsPlayer", "привет")
        assertTrue(
            "строка: $line",
            Regex("""\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3} D/TtsPlayer: привет\n""").matches(
                line,
            ),
        )
    }

    @Test
    fun `format подставляет App когда тег null`() {
        val line = FileLogFormatter.format(0L, Log.INFO, null, "x")
        assertTrue(line.contains(" I/App: x"))
    }

    @Test
    fun `format отображает уровни буквами`() {
        assertTrue(FileLogFormatter.format(0L, Log.WARN, "t", "m").contains(" W/"))
        assertTrue(FileLogFormatter.format(0L, Log.ERROR, "t", "m").contains(" E/"))
        assertTrue(FileLogFormatter.format(0L, Log.ASSERT, "t", "m").contains(" A/"))
    }

    @Test
    fun `shouldRotate срабатывает при превышении лимита`() {
        assertFalse(FileLogFormatter.shouldRotate(0, 100, maxBytes = 1000))
        assertFalse(FileLogFormatter.shouldRotate(900, 100, maxBytes = 1000))
        assertTrue(FileLogFormatter.shouldRotate(901, 100, maxBytes = 1000))
    }

    @Test
    fun `MAX_BYTES равен двум мегабайтам`() {
        assertEquals(2L * 1024 * 1024, FileLogFormatter.MAX_BYTES)
    }
}
