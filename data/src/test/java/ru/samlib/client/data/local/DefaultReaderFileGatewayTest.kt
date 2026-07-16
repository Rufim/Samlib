package ru.samlib.client.data.local

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultReaderFileGatewayTest {
    private val gateway = DefaultReaderFileGateway(ApplicationProvider.getApplicationContext())

    @Test
    fun `расширение берётся из реального файла, если у названия книги его нет`() {
        val name =
            gateway.resolveSharedFileName(
                sourceUri = "file:///data/user/0/ru.samlib.client/files/library/abc123_book.fb2",
                displayName = "Моя чудесная книга",
            )
        assertEquals("Моя чудесная книга.fb2", name)
    }

    @Test
    fun `имя без названия книги берётся из исходного файла целиком`() {
        val name =
            gateway.resolveSharedFileName(
                sourceUri = "file:///data/user/0/ru.samlib.client/files/library/abc123_book.fb2",
                displayName = null,
            )
        assertEquals("abc123_book.fb2", name)
    }

    @Test
    fun `известный формат книги важнее имени исходного файла без расширения`() {
        // Книга пришла через «Поделиться» — сторонний провайдер не сообщил DISPLAY_NAME
        // с расширением, поэтому копия на диске называется просто хешем без ".fb2".
        val name =
            gateway.resolveSharedFileName(
                sourceUri = "file:///data/user/0/ru.samlib.client/files/library/abc123def456",
                displayName = "Хроники Амбера",
                format = "fb2",
            )
        assertEquals("Хроники Амбера.fb2", name)
    }

    @Test
    fun `составное расширение fb2_zip добавляется полностью`() {
        val name =
            gateway.resolveSharedFileName(
                sourceUri = "file:///data/user/0/ru.samlib.client/files/library/abc123def456",
                displayName = "Книга",
                format = "fb2.zip",
            )
        assertEquals("Книга.fb2.zip", name)
    }

    @Test
    fun `недопустимые в имени файла символы вырезаются из названия книги`() {
        // Заголовок из тегов fb2 может содержать двоеточие и другие символы, запрещённые
        // в именах файлов на эмулированном хранилище (например AlReaderX падает с EPERM
        // при попытке материализовать полученный файл под таким именем).
        val name =
            gateway.resolveSharedFileName(
                sourceUri = "file:///data/user/0/ru.samlib.client/files/library/abc123def456",
                displayName = "Правила ужасов: Я начал с упрямым заявлением, что я не умер",
                format = "fb2",
            )
        assertEquals("Правила ужасов Я начал с упрямым заявлением что я не умер.fb2", name)
    }

    @Test
    fun `mimeOf распознаёт fb2 и составной fb2_zip`() {
        assertEquals("application/xml", gateway.mimeOf("Книга.fb2"))
        assertEquals("application/zip", gateway.mimeOf("Книга.fb2.zip"))
        assertEquals(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            gateway.mimeOf("Книга.docx"),
        )
    }
}
