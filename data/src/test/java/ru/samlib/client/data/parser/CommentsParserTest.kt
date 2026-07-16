package ru.samlib.client.data.parser

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.Charset

class CommentsParserTest {
    private fun fixture(name: String): String =
        javaClass
            .getResourceAsStream("/fixtures/$name")!!
            .readBytes()
            .toString(Charset.forName("windows-1251"))

    @Test
    fun `парсит комментарии`() {
        val comments = CommentsParser().parse(fixture("comment_page.html"))

        assertTrue("непустой список", comments.size >= 5)
        val c = comments.first()
        assertTrue("есть номер", c.number > 0)
        assertTrue("есть автор", !c.nickName.isNullOrBlank())
        assertTrue("есть дата", !c.date.isNullOrBlank())
        assertTrue("есть текст", c.body.isNotBlank())
    }

    @Test
    fun `у ответов определяется родитель`() {
        val comments = CommentsParser().parse(fixture("comment_page.html"))
        val replies = comments.filter { it.parentNumber != null }
        assertTrue("есть ответы с родителем", replies.isNotEmpty())
        // родитель всегда с меньшим номером
        assertTrue("родитель раньше ответа", replies.all { it.parentNumber!! < it.number })
    }

    @Test
    fun `цепочка цитат сохраняет вложенность`() {
        val comments = CommentsParser().parse(fixture("comment_page.html"))
        // в теле есть строки-цитаты, в т.ч. вложенный уровень «> >»
        assertTrue(
            "есть строки-цитаты",
            comments.any { c -> c.body.lines().any { it.trimStart().startsWith(">") } },
        )
        assertTrue(
            "есть вложенный уровень цитаты",
            comments.any { c ->
                c.body.lines().any {
                    it
                        .trimStart()
                        .removePrefix(">")
                        .trimStart()
                        .startsWith(">")
                }
            },
        )
    }

    @Test
    fun `номера уникальны и тело без html-тегов`() {
        val comments = CommentsParser().parse(fixture("comment_page.html"))
        assertTrue("номера различны", comments.map { it.number }.toSet().size == comments.size)
        assertTrue(
            "нет тегов в теле",
            comments.none { it.body.contains("<") && it.body.contains(">") },
        )
    }
}
