package ru.samlib.client.data.parser

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.Charset

class WorkRatingParserTest {
    private fun fixture(name: String): String =
        javaClass
            .getResourceAsStream("/fixtures/$name")!!
            .readBytes()
            .toString(Charset.forName("windows-1251"))

    @Test
    fun `парсит обзоры (литобзор)`() {
        val works = WorkRatingParser().parse(fixture("janr_litreview.html"))

        assertTrue("непустой список", works.size > 5)
        val w = works.first()
        assertTrue("название", !w.title.isNullOrBlank())
        assertTrue("ссылка .shtml", w.link.contains(".shtml"))
        assertTrue("автор указан", !w.authorShortName.isNullOrBlank())
        assertTrue("автор-ссылка", w.authorLink?.startsWith("/") == true)
        assertTrue("у кого-то есть оценка", works.any { it.rate != null && (it.votes ?: 0) > 0 })
    }

    @Test
    fun `парсит избранное (expert) с экспертной оценкой`() {
        val works = WorkRatingParser().parse(fixture("rating_expert.html"))

        assertTrue("непустой список", works.size > 5)
        assertTrue(
            "есть экспертная оценка",
            works.any { it.expertRate != null && (it.expertVotes ?: 0) > 0 },
        )
        assertTrue("есть обычная оценка", works.any { it.rate != null })
        val w = works.first()
        assertTrue("название", !w.title.isNullOrBlank())
        assertTrue("автор", !w.authorShortName.isNullOrBlank())
    }
}
