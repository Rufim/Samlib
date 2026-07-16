package ru.samlib.client.data.remote

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.samlib.client.domain.repository.UploadFile
import java.nio.charset.Charset

class SamlibHttpClientTest {
    @Test
    fun `декодирует windows-1251 в кириллицу и шлёт User-Agent`() =
        runTest {
            var sentUserAgent: String? = null
            val cyrillic = "Привет, Самиздат"
            val bytes = cyrillic.toByteArray(Charset.forName("windows-1251"))
            val engine =
                MockEngine { request ->
                    sentUserAgent = request.headers[HttpHeaders.UserAgent]
                    respond(
                        content = ByteReadChannel(bytes),
                        headers = headersOf(HttpHeaders.ContentType, "text/html; charset=windows-1251"),
                    )
                }
            val client = SamlibHttpClient(engine)

            val html = client.getPage("/long.shtml")

            assertEquals(cyrillic, html.trim())
            assertTrue(sentUserAgent?.contains("Firefox") == true)
        }

    @Test
    fun `postWorkForm собирает браузерный multipart в cp1251`() =
        runTest {
            var contentType: String? = null
            var body: ByteArray? = null
            val engine =
                MockEngine { request ->
                    contentType = request.headers[HttpHeaders.ContentType]
                        ?: request.body.contentType?.toString()
                    body = request.body.toByteArray()
                    respond(
                        content = ByteReadChannel(ByteArray(0)),
                        status = HttpStatusCode.Found,
                        headers = headersOf(HttpHeaders.Location, "/editors/k/kazancew_d/"),
                    )
                }
            val client = SamlibHttpClient(engine)

            val error =
                client.postWorkForm(
                    fields = listOf("DIR" to "k/kazancew_d", "TITLE" to "Проверка Ъё", "CODESET" to "auto"),
                    text = "Первая строка ёЁъЪ.",
                    cookie = "NAME=rufim",
                )

            // 302 → успех
            assertEquals(null, error)
            assertTrue(contentType!!.startsWith("multipart/form-data; boundary="))
            val boundary = contentType!!.substringAfter("boundary=")
            val cp1251 = Charset.forName("windows-1251")
            val raw = String(body!!, Charsets.ISO_8859_1)
            // текстовые части — байты cp1251 в порядке передачи
            val title = String("Проверка Ъё".toByteArray(cp1251), Charsets.ISO_8859_1)
            val text = String("Первая строка ёЁъЪ.".toByteArray(cp1251), Charsets.ISO_8859_1)
            assertTrue(raw.contains("Content-Disposition: form-data; name=\"TITLE\"\r\n\r\n$title\r\n"))
            assertTrue(raw.contains("Content-Disposition: form-data; name=\"TEXT\"\r\n\r\n$text\r\n"))
            // пустая файловая часть как у браузера — до TEXT
            val filePart =
                "Content-Disposition: form-data; name=\"FILE\"; filename=\"\"\r\n" +
                    "Content-Type: application/octet-stream\r\n\r\n\r\n"
            assertTrue(raw.contains(filePart))
            assertTrue(raw.indexOf(filePart) < raw.indexOf("name=\"TEXT\""))
            // нажатая кнопка Store — последняя часть, затем финальная граница
            assertTrue(
                raw.contains("Content-Disposition: form-data; name=\"OPERATION\"\r\n\r\nStore\r\n--$boundary--\r\n"),
            )
        }

    @Test
    fun `postWorkForm с файлом кладёт его содержимое в часть FILE`() =
        runTest {
            var body: ByteArray? = null
            val engine =
                MockEngine { request ->
                    body = request.body.toByteArray()
                    respond(
                        content = ByteReadChannel(ByteArray(0)),
                        status = HttpStatusCode.Found,
                        headers = headersOf(HttpHeaders.Location, "/editors/k/kazancew_d/"),
                    )
                }
            val client = SamlibHttpClient(engine)
            val cp1251 = Charset.forName("windows-1251")
            val fileBytes = "Текст из файла ёЁ".toByteArray(cp1251)

            val error =
                client.postWorkForm(
                    fields = listOf("DIR" to "k/kazancew_d"),
                    text = "",
                    file = UploadFile("story.txt", fileBytes, "text/plain"),
                    cookie = "",
                )

            assertEquals(null, error)
            val raw = String(body!!, Charsets.ISO_8859_1)
            val filePart =
                "Content-Disposition: form-data; name=\"FILE\"; filename=\"story.txt\"\r\n" +
                    "Content-Type: text/plain\r\n\r\n" + String(fileBytes, Charsets.ISO_8859_1) + "\r\n"
            assertTrue(raw.contains(filePart))
            // пустая текстовая часть всё равно присутствует и идёт после файла
            assertTrue(raw.contains("Content-Disposition: form-data; name=\"TEXT\"\r\n\r\n\r\n"))
            assertTrue(raw.indexOf(filePart) < raw.indexOf("name=\"TEXT\""))
        }

    @Test
    fun `postWorkForm сохраняет порядок полей формы - поля, FILE, TEXT, OPERATION`() =
        runTest {
            // Контракт с CGI самиздата: части идут в порядке полей формы, пустая FILE-часть
            // перед TEXT, нажатая кнопка OPERATION=Store — последней. Нарушение порядка
            // сервер молча отвергает (решённый блокер создания произведений).
            var body: ByteArray? = null
            val engine =
                MockEngine { request ->
                    body = request.body.toByteArray()
                    respond(
                        content = ByteReadChannel(ByteArray(0)),
                        status = HttpStatusCode.Found,
                        headers = headersOf(HttpHeaders.Location, "/editors/"),
                    )
                }
            val client = SamlibHttpClient(engine)

            client.postWorkForm(
                fields = listOf("DIR" to "x", "TITLE" to "t", "CODESET" to "auto"),
                text = "тело",
                cookie = "",
            )

            val raw = String(body!!, Charsets.ISO_8859_1)
            val order =
                listOf(
                    "name=\"DIR\"",
                    "name=\"TITLE\"",
                    "name=\"CODESET\"",
                    "name=\"FILE\"",
                    "name=\"TEXT\"",
                    "name=\"OPERATION\"",
                )
            val positions = order.map { raw.indexOf(it) }
            assertTrue("часть не найдена: $positions", positions.all { it >= 0 })
            assertEquals("нарушен порядок частей", positions, positions.sorted())
        }

    @Test
    fun `postWorkForm возвращает сообщение сервера при отказе`() =
        runTest {
            val cp1251 = Charset.forName("windows-1251")
            val page = "<html><body>Самиздат: Управление <b>Ошибка: не передали текст</b></body></html>"
            val engine =
                MockEngine {
                    respond(
                        content = ByteReadChannel(page.toByteArray(cp1251)),
                        headers = headersOf(HttpHeaders.ContentType, "text/html; charset=windows-1251"),
                    )
                }
            val client = SamlibHttpClient(engine)

            val error = client.postWorkForm(fields = emptyList(), text = "т", cookie = "")

            assertTrue(error!!.contains("не передали текст"))
        }
}
