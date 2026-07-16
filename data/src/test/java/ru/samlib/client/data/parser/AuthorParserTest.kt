package ru.samlib.client.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.nio.charset.Charset

class AuthorParserTest {
    private fun fixture(name: String): String =
        javaClass
            .getResourceAsStream("/fixtures/$name")!!
            .readBytes()
            .toString(Charset.forName("windows-1251"))

    private fun parseAuthor() = AuthorParser().parse(fixture("author.html"), "/m/melxnik_a_a/")

    @Test
    fun `парсит имя и ссылку автора`() {
        val author = parseAuthor()
        assertEquals("/m/melxnik_a_a/", author.link)
        assertEquals("Мельник Анатолий Анатольевич", author.fullName)
    }

    @Test
    fun `парсит статистику из инфо-таблицы`() {
        val author = parseAuthor()
        assertEquals(95, author.size)
        assertEquals(726, author.workCount)
        assertEquals(1769, author.views)
        assertNotNull("дата рождения", author.dateBirth)
        assertTrue("город не пуст", !author.address.isNullOrBlank())
    }

    @Test
    fun `парсит аннотацию раздела и рекомендации`() {
        val author = parseAuthor()
        assertTrue("аннотация раздела не пуста", !author.sectionAnnotation.isNullOrBlank())
        assertTrue("есть рекомендации", author.recommendations.isNotEmpty())
    }

    @Test
    fun `группирует работы по разделу Стихотворение`() {
        val author = parseAuthor()
        val cat = author.categories.firstOrNull { it.title == "Стихотворение" }
        assertNotNull("есть раздел Стихотворение", cat)
        assertTrue("в разделе есть работы", cat!!.works.isNotEmpty())
        val w = cat.works.first()
        assertTrue("ссылка работы в каталоге автора", w.link.startsWith("/m/melxnik_a_a/"))
        assertTrue("у работы есть заголовок", !w.title.isNullOrBlank())
    }

    @Test
    fun `в разделе Стихотворение все работы (без потери первой)`() {
        val html = fixture("author.html")
        val author = AuthorParser().parse(html, "/m/melxnik_a_a/")
        val cat = author.categories.first { it.title == "Стихотворение" }
        assertEquals(726, cat.works.size)
        assertTrue(
            "первая работа раздела присутствует",
            cat.works.any { it.title == "А снег летит наискосок" },
        )
    }

    @Test
    fun `у работы с комментариями заполнен commentCount`() {
        val author = parseAuthor()
        assertTrue(
            "хотя бы у одной работы есть commentCount",
            author.works.any { it.commentCount != null },
        )
    }

    @Test
    fun `битый html бросает ParseException`() {
        assertThrows(ParseException::class.java) {
            AuthorParser().parse("<html><body>пусто</body></html>", "/m/melxnik_a_a/")
        }
    }

    @Test
    fun `parseWorksFromFragment разбирает работы из фрагмента без маркеров`() {
        val fragment =
            """
            <DL><DT><li><A HREF=work1.shtml><b>Работа 1</b></A> &nbsp; <b>3k</b> &nbsp; <small> Проза </small></DL>
            """.trimIndent()
        val works = AuthorParser().parseWorksFromFragment(fragment, "/m/melxnik_a_a/")
        assertEquals(1, works.size)
        assertEquals("Работа 1", works.first().title)
        assertEquals("/m/melxnik_a_a/work1.shtml", works.first().link)
        assertEquals(3, works.first().size)
    }

    @Test
    fun `парсит оценку и очищенный жанр у работы`() {
        val fragment =
            """
            <!-- Блок ссылок на произведения -->
            <DL><DT><li><A HREF=w.shtml><b>Работа</b></A> &nbsp; <b>475k 6.50*10</b> &nbsp;
            <small>Оценка: История <A HREF=/comment/t/test/w>Комментарии: 5</A></small></DL>
            <!-- Подножие -->
            """.trimIndent()
        val works = AuthorParser().parseWorksFromFragment(fragment, "/t/test/")
        assertEquals(1, works.size)
        val w = works.first()
        assertEquals(475, w.size)
        assertEquals(BigDecimal("6.50"), w.rate)
        assertEquals(10, w.votes)
        assertEquals("История", w.customGenres)
        assertEquals(5, w.commentCount)
    }

    @Test
    fun `аннотация работы сохраняет html со ссылкой`() {
        val fragment =
            """
            <!-- Блок ссылок на произведения -->
            <DL><DT><li><A HREF=w.shtml><b>Работа</b></A> &nbsp; <b>1k</b> &nbsp;
            <small>Проза</small><DD>Описание <a href="http://ex.com">тут</a></DD></DL>
            <!-- Подножие -->
            """.trimIndent()
        val works = AuthorParser().parseWorksFromFragment(fragment, "/t/test/")
        assertTrue("annotation содержит тег <a", works.first().annotation!!.contains("<a"))
    }

    @Test
    fun `парсит аватар из шапки`() {
        val html =
            """
            <html><body><center><h3>Тест Автор</h3></center>
            <!-- Блок шапки -->
            <img src=/i/t/test/photo2.jpg>
            <ul><li><b>Объем:</b> 10k/2</li></ul>
            <!-- Блок управления разделом -->
            <!-- Блок ссылок на произведения -->
            <DL><DT><li><A HREF=w.shtml><b>Работа</b></A> <b>0k</b> <small>Проза</small></DL>
            <!-- Подножие -->
            </body></html>
            """.trimIndent()
        val author = AuthorParser().parse(html, "/t/test/")
        assertEquals("http://samlib.ru/i/t/test/photo2.jpg", author.avatarUrl)
    }

    @Test
    fun `без картинки аватар null`() {
        val html =
            """
            <html><body><center><h3>Тест</h3></center>
            <!-- Блок шапки --><ul><li><b>Объем:</b> 1k/1</li></ul>
            <!-- Блок управления разделом -->
            <!-- Блок ссылок на произведения -->
            <DL><DT><li><A HREF=w.shtml><b>Работа</b></A> <b>0k</b> <small>Проза</small></DL>
            <!-- Подножие --></body></html>
            """.trimIndent()
        assertEquals(null, AuthorParser().parse(html, "/t/test/").avatarUrl)
    }
}
