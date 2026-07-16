package ru.samlib.client.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Locale

class WorkParserTest {
    private fun fixture(name: String): String =
        javaClass
            .getResourceAsStream("/fixtures/$name")!!
            .readBytes()
            .toString(Charset.forName("windows-1251"))

    @Test
    fun `парсит метаданные и тело произведения`() {
        val html = fixture("work.html")

        val work = WorkParser().parse(html, "/m/melxnik_a_a/letnijdozhdik.shtml")

        assertEquals("/m/melxnik_a_a/letnijdozhdik.shtml", work.link)
        assertEquals("Летний дождик", work.title)
        assertTrue("тело не пусто", work.rawContent.isNotBlank())
        assertTrue("тело содержит текст произведения", work.rawContent.contains("дождик"))
    }

    @Test
    fun `извлекает автора и жанр для истории`() {
        val work = WorkParser().parse(fixture("work.html"), "/m/melxnik_a_a/letnijdozhdik.shtml")
        assertEquals("Мельник Анатолий Анатольевич", work.authorShortName)
        assertEquals("Поэзия", work.customGenres)
    }

    @Test
    fun `парсит дату изменения и размер из подвала страницы`() {
        val html =
            """
            <html><head><title>Автор Тест. Книга</title></head><body>
            <!----------- Собственно произведение --------------->
            <p>Текст книги.</p>
            <!--------- Блок описания произведения (слева внизу) ----------------------->
            &copy; Copyright <a href=mailto:x@y.z>Автор Тест</a>
            Размещен: 09/11/2010, изменен: 15/03/2024. 186k. Статистика.
            </body></html>
            """.trimIndent()
        val work = WorkParser().parse(html, "/t/test/kniga.shtml")
        val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        assertEquals("15/03/2024", fmt.format(work.updateDate!!))
        assertEquals(186, work.size)
    }
}
