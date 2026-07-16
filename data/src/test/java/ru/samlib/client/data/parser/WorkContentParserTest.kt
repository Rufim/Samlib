package ru.samlib.client.data.parser

import org.junit.Assert.assertTrue
import org.junit.Test
import ru.samlib.client.domain.model.ContentBlock
import java.nio.charset.Charset

class WorkContentParserTest {
    private val parser = DefaultWorkContentParser()

    @Test
    fun `разбирает синтетический html на блоки нужных типов`() {
        val html =
            """
            <dd>Обычный <b>жирный</b> и <i>курсив</i> абзац.</dd>
            <h2>Заголовок</h2>
            <pre>Строка 1
            Строка 2</pre>
            <hr>
            <img src="pic.jpg">
            """.trimIndent()

        val blocks = parser.parse(html)

        assertTrue(blocks.any { it is ContentBlock.Paragraph })
        assertTrue(blocks.any { it is ContentBlock.Heading && it.level == 2 })
        assertTrue(blocks.any { it is ContentBlock.Preformatted && it.text.contains("Строка 1") })
        assertTrue(blocks.any { it is ContentBlock.Divider })
        assertTrue(blocks.any { it is ContentBlock.ImageBlock && it.url == "http://samlib.ru/pic.jpg" })
    }

    @Test
    fun `таблица становится блоком Table с ячейками и жирным th`() {
        val html =
            """
            <table border=1>
            <tr><th>Имя</th><th>Число</th></tr>
            <tr><td><i>раз</i></td><td>1</td></tr>
            <tr><td>два</td><td>2</td></tr>
            </table>
            """.trimIndent()

        val blocks = parser.parse(html)

        val table = blocks.filterIsInstance<ContentBlock.Table>().single()
        assertTrue(table.rows.size == 3)
        assertTrue(table.rows[0][0] == "<b>Имя</b>")
        assertTrue(table.rows[1][0].contains("<i>"))
        assertTrue(table.rows[2] == listOf("два", "2"))
    }

    @Test
    fun `списки ul и ol превращаются в абзацы с маркерами`() {
        val html = "<ul><li>первый</li><li>второй</li></ul><ol><li>раз</li><li>два</li></ol>"

        val blocks = parser.parse(html).filterIsInstance<ContentBlock.Paragraph>()

        assertTrue(blocks.any { it.html == "• первый" })
        assertTrue(blocks.any { it.html == "• второй" })
        assertTrue(blocks.any { it.html == "1. раз" })
        assertTrue(blocks.any { it.html == "2. два" })
    }

    @Test
    fun `pre с разметкой разбирается на форматированные блоки, а не в plain-текст`() {
        // samlib заворачивает всё тело в <pre>; браузер применяет теги внутри
        val html =
            """
            <pre><b>жирный</b> <i>курсив</i>
            <h3>Заголовок</h3>
            <ul><li>один</li><li>два</li></ul>
            <table border=1><tr><td>a</td><td>b</td></tr></table></pre>
            """.trimIndent()

        val blocks = parser.parse(html)

        // не должно быть единого Preformatted со всем текстом
        assertTrue(blocks.none { it is ContentBlock.Preformatted })
        assertTrue(blocks.any { it is ContentBlock.Paragraph && it.html.contains("<b>жирный</b>") })
        assertTrue(blocks.any { it is ContentBlock.Heading && it.text == "Заголовок" })
        assertTrue(blocks.any { it is ContentBlock.Paragraph && it.html == "• один" })
        assertTrue(blocks.any { it is ContentBlock.Table })
    }

    @Test
    fun `чистый pre без тегов остаётся моноширинным блоком`() {
        val blocks = parser.parse("<pre>строка 1\nстрока 2</pre>")
        assertTrue(blocks.single() is ContentBlock.Preformatted)
    }

    @Test
    fun `реальная фикстура-стих даёт непустой контент с pre`() {
        val body =
            extractBody(
                javaClass
                    .getResourceAsStream("/fixtures/work.html")!!
                    .readBytes()
                    .toString(Charset.forName("windows-1251")),
            )
        val blocks = parser.parse(body)
        assertTrue(blocks.isNotEmpty())
        assertTrue(blocks.any { it is ContentBlock.Preformatted })
    }

    // тело как его извлекает WorkParser (между маркерами)
    private fun extractBody(html: String): String {
        val s = html.indexOf("Собственно произведение")
        val e = html.indexOf("Блок описания произведения (слева внизу)")
        if (s < 0 || e <= s) return html
        val after = html.indexOf("-->", s) + 3
        val end = html.lastIndexOf("<!--", e).let { if (it in (after + 1) until e) it else e }
        return html.substring(after, end)
    }
}
