package ru.samlib.client.data.parser

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.Charset

class SearchParserTest {
    private fun fixture(name: String): String =
        javaClass
            .getResourceAsStream("/fixtures/$name")!!
            .readBytes()
            .toString(Charset.forName("windows-1251"))

    @Test
    fun `парсит результаты поиска`() {
        val works = SearchParser().parse(fixture("search.html"))
        assertTrue("ожидаются результаты", works.isNotEmpty())
        val w = works.first()
        assertTrue("заголовок", !w.title.isNullOrBlank())
        assertTrue("ссылка работы", w.link.isNotBlank())
        assertTrue("автор", !w.authorShortName.isNullOrBlank() || !w.authorLink.isNullOrBlank())
    }
}
