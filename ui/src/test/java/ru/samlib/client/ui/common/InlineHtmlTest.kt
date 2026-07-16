package ru.samlib.client.ui.common

import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InlineHtmlTest {
    @Test
    fun `с listener ссылка a становится LinkAnnotation Url`() {
        val s =
            htmlToAnnotatedString(
                "до <a href=\"/x/y/z.shtml\">ссылка</a> после",
                linkListener = LinkInteractionListener {},
            )
        val links = s.getLinkAnnotations(0, s.length)
        assertEquals(1, links.size)
        assertEquals("/x/y/z.shtml", (links.first().item as LinkAnnotation.Url).url)
        assertEquals("до ссылка после", s.text)
    }

    @Test
    fun `без listener ссылка a рендерится как текст`() {
        val s = htmlToAnnotatedString("до <a href=\"/x/y/z.shtml\">ссылка</a> после")
        assertEquals(0, s.getLinkAnnotations(0, s.length).size)
        assertEquals("до ссылка после", s.text)
    }

    @Test
    fun `текст без тегов сохраняется`() {
        val s = htmlToAnnotatedString("простой текст")
        assertEquals("простой текст", s.text)
    }

    @Test
    fun `b даёт жирный span`() {
        val s = htmlToAnnotatedString("до <b>жир</b> после")
        assertTrue(s.text.contains("жир"))
        val bold = s.spanStyles.any { it.item.fontWeight == FontWeight.Bold }
        assertTrue(bold)
    }

    @Test
    fun `i даёт курсивный span`() {
        val s = htmlToAnnotatedString("<i>кур</i>")
        val italic = s.spanStyles.any { it.item.fontStyle == FontStyle.Italic }
        assertTrue(italic)
    }

    @Test
    fun `br даёт перенос строки`() {
        val s = htmlToAnnotatedString("a<br>b")
        assertTrue(s.text.contains("\n"))
    }

    @Test
    fun `голый http url в тексте становится ссылкой`() {
        val s =
            htmlToAnnotatedString(
                "см. http://samlib.ru/x/y/z.shtml тут",
                linkListener = LinkInteractionListener {},
            )
        val links = s.getLinkAnnotations(0, s.length)
        assertEquals(1, links.size)
        assertEquals("http://samlib.ru/x/y/z.shtml", (links.first().item as LinkAnnotation.Url).url)
    }

    @Test
    fun `голый samlib-путь становится ссылкой`() {
        val s =
            htmlToAnnotatedString(
                "ссылка /a/b/c.shtml конец",
                linkListener = LinkInteractionListener {},
            )
        val links = s.getLinkAnnotations(0, s.length)
        assertEquals(1, links.size)
        assertEquals("/a/b/c.shtml", (links.first().item as LinkAnnotation.Url).url)
    }

    @Test
    fun `без listener голый url остаётся текстом`() {
        val s = htmlToAnnotatedString("см. http://samlib.ru/x/y/z.shtml")
        assertEquals(0, s.getLinkAnnotations(0, s.length).size)
    }
}
