package ru.samlib.client.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.Charset

class CharsetDetectTest {
    private val cp1251 = Charset.forName("windows-1251")

    @Test
    fun `txt в cp1251 декодируется в кириллицу`() {
        val bytes = "Привет мир".toByteArray(cp1251)
        assertEquals("Привет мир", decodeBytes(bytes, isHtml = false))
    }

    @Test
    fun `txt в utf-8 декодируется корректно`() {
        val bytes = "Привет мир".toByteArray(Charsets.UTF_8)
        assertEquals("Привет мир", decodeBytes(bytes, isHtml = false))
    }

    @Test
    fun `html с meta windows-1251 декодируется как cp1251`() {
        val html = "<html><head><meta charset=windows-1251></head><body>Текст</body></html>"
        val bytes = html.toByteArray(cp1251)
        assertTrue(decodeBytes(bytes, isHtml = true).contains("Текст"))
    }
}
