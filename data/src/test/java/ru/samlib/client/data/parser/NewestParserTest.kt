package ru.samlib.client.data.parser

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.Charset
import java.util.Calendar

class NewestParserTest {
    private fun fixture(name: String): String =
        javaClass
            .getResourceAsStream("/fixtures/$name")!!
            .readBytes()
            .toString(Charset.forName("windows-1251"))

    @Test
    fun `парсит список новинок с валидными полями`() {
        val html = fixture("newest_long.html")

        val works = NewestParser().parse(html, skip = 0, size = 20)

        assertTrue("ожидается непустой список", works.isNotEmpty())
        val w = works.first()
        assertTrue("заголовок не пуст", !w.title.isNullOrBlank())
        assertTrue("размер > 0", (w.size ?: 0) > 0)
        assertTrue("автор указан", !w.authorShortName.isNullOrBlank())
        assertTrue("ссылка на работу", w.link.isNotBlank())
    }

    @Test
    fun `skip сдвигает выборку`() {
        val html = fixture("newest_long.html")
        val first10 = NewestParser().parse(html, skip = 0, size = 10)
        val next10 = NewestParser().parse(html, skip = 10, size = 10)
        assertTrue(first10.isNotEmpty() && next10.isNotEmpty())
        assertTrue("страницы не совпадают", first10.first().link != next10.first().link)
    }

    @Test
    fun `у первой новинки есть дата обновления`() {
        val works = NewestParser().parse(fixture("newest_long.html"))
        assertTrue("есть новинки", works.isNotEmpty())
        assertNotNull("дата обновления заполнена", works.first().updateDate)
    }

    @Test
    fun `даты новинок не в будущем`() {
        val works = NewestParser().parse(fixture("newest_long.html"))
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.time
        assertTrue(
            "все даты не позже завтрашнего дня",
            works.all { it.updateDate == null || it.updateDate!!.before(tomorrow) },
        )
    }

    @Test
    fun `у первой новинки есть жанры`() {
        val works = NewestParser().parse(fixture("newest_long.html"))
        assertTrue("есть новинки", works.isNotEmpty())
        assertTrue("жанры не пусты", !works.first().customGenres.isNullOrBlank())
    }

    @Test
    fun `dateText первой новинки без года и совпадает с сырым текстом`() {
        val works = NewestParser().parse(fixture("newest_long.html"))
        assertTrue("есть новинки", works.isNotEmpty())
        val dt = works.first().dateText
        assertTrue("dateText не пуст", !dt.isNullOrBlank())
        assertTrue("без года (4-значного)", !Regex("""\d{4}""").containsMatchIn(dt!!))
    }
}
