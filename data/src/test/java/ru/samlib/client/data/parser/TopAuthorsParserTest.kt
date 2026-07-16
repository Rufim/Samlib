package ru.samlib.client.data.parser

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.Charset

class TopAuthorsParserTest {
    private fun fixture(name: String): String =
        javaClass
            .getResourceAsStream("/fixtures/$name")!!
            .readBytes()
            .toString(Charset.forName("windows-1251"))

    @Test
    fun `парсит топ авторов`() {
        val authors = TopAuthorsParser().parse(fixture("rating_hits.html"))

        assertTrue("ожидается непустой список", authors.size > 10)
        val a = authors.first()
        assertTrue("имя не пусто", !a.fullName.isNullOrBlank())
        assertTrue("ссылка на автора", a.link.startsWith("/"))
        assertTrue("просмотры > 0", (a.views ?: 0) > 0)
    }

    @Test
    fun `у автора заполнены метрики и описание`() {
        val authors = TopAuthorsParser().parse(fixture("rating_hits.html"))
        // хотя бы у части авторов есть число работ и описание
        assertTrue("у кого-то есть workCount", authors.any { (it.workCount ?: 0) > 0 })
        assertTrue("у кого-то есть описание", authors.any { !it.annotation.isNullOrBlank() })
    }
}
