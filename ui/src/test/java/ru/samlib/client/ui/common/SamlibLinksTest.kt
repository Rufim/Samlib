package ru.samlib.client.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class SamlibLinksTest {
    @Test
    fun `shtml ссылка samlib открывается как произведение`() {
        assertEquals(
            SamlibTarget.Work("/x/y/z.shtml"),
            classifySamlibLink("http://samlib.ru/x/y/z.shtml"),
        )
    }

    @Test
    fun `относительная shtml как произведение`() {
        assertEquals(SamlibTarget.Work("/x/y/z.shtml"), classifySamlibLink("/x/y/z.shtml"))
    }

    @Test
    fun `каталог samlib как автор`() {
        assertEquals(SamlibTarget.Author("/x/y/"), classifySamlibLink("http://samlib.ru/x/y/"))
    }

    @Test
    fun `comment как комментарии`() {
        assertEquals(
            SamlibTarget.Comments("/comment/x/y/z"),
            classifySamlibLink("http://samlib.ru/comment/x/y/z"),
        )
    }

    @Test
    fun `img как иллюстрации`() {
        assertEquals(
            SamlibTarget.Illustrations("/img/x/y/z/"),
            classifySamlibLink("http://samlib.ru/img/x/y/z/"),
        )
    }

    @Test
    fun `служебный раздел как неподдерживаемый`() {
        assertEquals(
            SamlibTarget.Unsupported("/rating/comment/"),
            classifySamlibLink("http://samlib.ru/rating/comment/"),
        )
    }

    @Test
    fun `внешняя ссылка как external`() {
        assertEquals(
            SamlibTarget.External("http://example.com/a"),
            classifySamlibLink("http://example.com/a"),
        )
    }

    @Test
    fun `mailto ссылка внешняя`() {
        assertEquals(SamlibTarget.External("mailto:a@b.com"), classifySamlibLink("mailto:a@b.com"))
    }

    @Test
    fun `спуф-домен не считается samlib`() {
        val href = "http://evil-samlib.ru.attacker.example/x"
        assertEquals(SamlibTarget.External(href), classifySamlibLink(href))
    }

    @Test
    fun `samlib в верхнем регистре как автор`() {
        assertEquals(SamlibTarget.Author("/x/y/"), classifySamlibLink("http://SAMLIB.RU/x/y/"))
    }

    @Test
    fun `bare-relative резолвится по каталогу автора`() {
        assertEquals(
            SamlibTarget.Work("/m/melxnik_a_a/text.shtml"),
            classifySamlibLink("text.shtml", authorDir = "/m/melxnik_a_a/"),
        )
    }
}
