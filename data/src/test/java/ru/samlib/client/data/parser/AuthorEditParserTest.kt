package ru.samlib.client.data.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class AuthorEditParserTest {
    private fun fixture(name: String): String =
        javaClass.getResourceAsStream("/fixtures/$name")!!.readBytes().toString(Charsets.UTF_8)

    @Test
    fun `форма о себе разбирается в поля`() {
        val fields = AuthorEditParser().parseForm(fixture("about_form.html"))
        assertEquals("k/kazancew_d", fields["DIR"])
        assertEquals("about", fields["STORE_FILE"])
        assertEquals("Россия", fields["COUNTRY"])
        assertEquals("Москва", fields["TOWN"])
        assertEquals("04/05/1990", fields["BD"])
        assertEquals("Привет! Пишу фантастику.\nВторая строка.", fields["BODY"])
        // submit-кнопка OPERATION не попадает в поля
        assertEquals(null, fields["OPERATION"])
    }

    @Test
    fun `форма раздела разбирается вместе с select и checkbox`() {
        val fields = AuthorEditParser().parseForm(fixture("dir_form.html"))
        assertEquals("k/kazancew_d", fields["DIR"])
        assertEquals("Казанцев Дмитрий", fields["AUTHOR"])
        assertEquals("Лаборатория.", fields["TITLE"])
        assertEquals("0rufim0@gmail.com", fields["EMAIL"])
        // select: берётся значение выбранного option
        assertEquals("2", fields["READ_LEVEL"])
        assertEquals("4", fields["WRITE_LEVEL"])
        assertEquals("99", fields["NO_COMMENT_DIR"])
        // отмеченный checkbox сохраняется, неотмеченный — пропускается
        assertEquals("1", fields["SUBS_ALL"])
        assertEquals(null, fields["NOTIFY"])
        assertEquals("Аннотация раздела в две\nстроки.", fields["ANNOT"])
        assertEquals(null, fields["OPERATION"])
    }

    @Test
    fun `форма нового произведения разбирается в поля с дефолтами селектов`() {
        val fields = AuthorEditParser().parseForm(fixture("new_book_form.html"))
        assertEquals("k/kazancew_d", fields["DIR"])
        assertEquals("rufim", fields["OWNER"])
        assertEquals("STORE_FILE,TITLE,DATE,JANR,AUTHOR", fields["REQUIRED"])
        assertEquals("Казанцев Дмитрий", fields["AUTHOR"])
        assertEquals("04/07/2026", fields["DATE"])
        // селекты: выбранный option, иначе первый
        assertEquals("2", fields["READ_LEVEL"])
        assertEquals("4", fields["WRITE_LEVEL"])
        assertEquals("0", fields["ADD_TYPE"])
        assertEquals("auto", fields["CODESET"])
    }

    @Test
    fun `форма редактирования произведения содержит текущие метаданные`() {
        val fields = AuthorEditParser().parseForm(fixture("edit_book_form.html"))
        assertEquals("Yes", fields["REPLACE"])
        assertEquals("fprobe1", fields["STORE_FILE"])
        assertEquals("Проба файла ёё", fields["TITLE"])
        // селекты с selected-опциями
        assertEquals("3", fields["TYPE"])
        assertEquals("5", fields["JANR"])
        assertEquals("0", fields["ADD_TYPE"])
    }

    @Test
    fun `страница подтверждения удаления отдаёт delhash`() {
        val fields = AuthorEditParser().parseForm(fixture("delete_confirm.html"))
        assertEquals("delete_object", fields["OPERATION"])
        assertEquals("k/kazancew_d", fields["DIR"])
        assertEquals("fprobe1", fields["FILE"])
        assertEquals("19S2VUGpu1Yc2", fields["delhash"])
    }

    @Test
    fun `опции селектов формы произведения с человеческими подписями`() {
        val selects = AuthorEditParser().parseSelectOptions(fixture("new_book_form.html"))
        val types = selects["TYPE"]!!
        assertEquals("1", types[1].value)
        // транслит в скобках отброшен
        assertEquals("Роман", types[1].label)
        val janrs = selects["JANR"]!!
        assertEquals("Проза", janrs.first { it.value == "5" }.label)
        val groups = selects["ADD_TYPE"]!!
        assertEquals("Нет", groups[0].label)
        // латинская подпись без кириллицы не теряется
        assertEquals("Child of Light", groups.first { it.value == "2" }.label)
    }
}
