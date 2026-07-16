package ru.samlib.client.data.parser

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.Charset

class DiscussionParserTest {
    private fun fixture(name: String): String =
        javaClass
            .getResourceAsStream("/fixtures/$name")!!
            .readBytes()
            .toString(Charset.forName("windows-1251"))

    @Test
    fun `парсит обсуждения`() {
        val items = DiscussionParser().parse(fixture("rating_comment.html"))

        assertTrue("непустой список", items.size > 10)
        val d = items.first()
        assertTrue("название произведения", !d.work.title.isNullOrBlank())
        assertTrue("ссылка на произведение .shtml", d.work.link.endsWith(".shtml"))
        assertTrue("автор", !d.author.fullName.isNullOrBlank())
        assertTrue("счётчик комментариев > 0", (d.count ?: 0) > 0)
        assertTrue("есть дата последнего", !d.lastOne.isNullOrBlank())
    }

    @Test
    fun `заголовок таблицы не попадает в результат`() {
        val items = DiscussionParser().parse(fixture("rating_comment.html"))
        assertTrue("нет строки-заголовка", items.none { it.work.title == "Произведение" })
    }
}
