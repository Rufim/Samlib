package ru.samlib.client.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.samlib.client.data.parser.DefaultWorkContentParser
import ru.samlib.client.domain.model.ContentBlock
import java.nio.charset.Charset

class LocalFileRepositoryParseTest {
    private val parser = DefaultWorkContentParser()

    @Test
    fun `txt разбивается на абзацы по пустым строкам`() {
        val txt = "Первый абзац.\nВторая строка.\n\nВторой абзац."
        val doc =
            parseLocalDocument(txt.toByteArray(Charsets.UTF_8), "file.txt", isHtml = false, parser)
        val paragraphs = doc.blocks.filterIsInstance<ContentBlock.Paragraph>()
        assertEquals(2, paragraphs.size)
        assertTrue(paragraphs[0].html.contains("<br>"))
    }

    @Test
    fun `cp1251 txt даёт корректную кириллицу`() {
        val bytes = "Пример текста".toByteArray(Charset.forName("windows-1251"))
        val doc = parseLocalDocument(bytes, "file.txt", isHtml = false, parser)
        assertTrue(
            doc.blocks
                .filterIsInstance<ContentBlock.Paragraph>()
                .first()
                .html
                .contains("Пример"),
        )
    }

    @Test
    fun `html разбирается в блоки`() {
        val html = "<html><body><dd>Абзац</dd><pre>Стих</pre></body></html>"
        val doc =
            parseLocalDocument(html.toByteArray(Charsets.UTF_8), "file.html", isHtml = true, parser)
        assertTrue(doc.blocks.any { it is ContentBlock.Paragraph })
        assertTrue(doc.blocks.any { it is ContentBlock.Preformatted })
    }

    @Test
    fun `заголовок сохраняется`() {
        val doc = parseLocalDocument("текст".toByteArray(), "рассказ.txt", isHtml = false, parser)
        assertEquals("рассказ.txt", doc.title)
    }

    @Test
    fun `samlib-html извлекает тело и метаданные из title`() {
        val html =
            "<html><head><title>Иванов Иван. Мой рассказ</title></head><body>" +
                "<div>меню сайта</div>" +
                "<!-- Собственно произведение --><dd>Текст произведения</dd>" +
                "<!-- Блок описания произведения (слева внизу) --><div>подвал</div>" +
                "</body></html>"
        val doc =
            parseLocalDocument(html.toByteArray(Charsets.UTF_8), "file.html", isHtml = true, parser)
        assertEquals("Мой рассказ", doc.title)
        assertEquals("Иванов Иван", doc.author)
        val text =
            doc.blocks.filterIsInstance<ContentBlock.Paragraph>().joinToString(" ") { it.html }
        assertTrue(text.contains("Текст произведения"))
        assertTrue(
            "служебные блоки не попадают в контент",
            !text.contains("меню сайта") && !text.contains("подвал"),
        )
    }

    @Test
    fun `не-samlib html идёт в режим чтения без скриптов и навигации`() {
        val html =
            "<html><body><nav>навигация</nav><script>alert(1)</script>" +
                "<article><p>Основной текст статьи.</p></article><footer>подвал</footer></body></html>"
        val doc =
            parseLocalDocument(html.toByteArray(Charsets.UTF_8), "page.html", isHtml = true, parser)
        val text =
            doc.blocks.filterIsInstance<ContentBlock.Paragraph>().joinToString(" ") { it.html }
        assertTrue(text.contains("Основной текст"))
        assertTrue(
            "нет навигации/скриптов/подвала",
            !text.contains("навигация") && !text.contains("alert") && !text.contains("подвал"),
        )
    }
}
