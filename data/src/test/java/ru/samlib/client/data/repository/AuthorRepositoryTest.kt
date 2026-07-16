package ru.samlib.client.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.samlib.client.data.parser.AuthorParser
import ru.samlib.client.data.parser.WorkParser
import ru.samlib.client.data.remote.SamlibHttpClient
import ru.samlib.client.data.testfixtures.FakeAuthorDao
import ru.samlib.client.data.testfixtures.FakeObservedWorkDao
import ru.samlib.client.domain.repository.AuthorRepository
import ru.samlib.client.domain.repository.SamlibResult
import java.nio.charset.Charset

class AuthorRepositoryTest {
    // Тело ответа кодируем в windows-1251, т.к. SamlibHttpClient.getPage декодирует именно этой
    // кодировкой; ISO-8859-1 не умеет кириллицу и портит текст в "?????".
    private val cp1251: Charset = Charset.forName("windows-1251")

    private val mainHtml =
        """
        <html><body><center><h3>Тест Автор</h3></center>
        <!-- ------- Блок ссылок на произведения ---------- -->
        </small><p><a href=index_1.shtml>Раздел А</a><br>
        <!-- ------- Подножие ----------------------------- -->
        </body></html>
        """.trimIndent()

    private val subHtml =
        """
        <html><body>
        <!-- ------- Блок ссылок на произведения ---------- -->
        <DL><DT><li><A HREF=work1.shtml><b>Работа 1</b></A> &nbsp; <b>2k</b> &nbsp; <small> Проза </small></DL>
        <!-- ------- Подножие ----------------------------- -->
        </body></html>
        """.trimIndent()

    private fun repo(engine: MockEngine): AuthorRepository =
        DefaultAuthorRepository(
            SamlibHttpClient(engine),
            AuthorParser(),
            FakeAuthorDao(),
            FakeObservedWorkDao(),
            WorkParser(),
        )

    @Test
    fun `догружает работы раздела-ссылки`() =
        runTest {
            val engine =
                MockEngine { request ->
                    val body = if (request.url.encodedPath.endsWith("index_1.shtml")) subHtml else mainHtml
                    respond(ByteReadChannel(body.toByteArray(cp1251)))
                }
            val result = repo(engine).getAuthor("/t/test/")
            assertTrue(result is SamlibResult.Success)
            val author = (result as SamlibResult.Success).data
            val cat = author.categories.first { it.title == "Раздел А" }
            assertEquals(1, cat.works.size)
            assertEquals("Работа 1", cat.works.first().title)
        }

    @Test
    fun `ошибка догрузки не роняет автора`() =
        runTest {
            val engine =
                MockEngine { request ->
                    if (request.url.encodedPath.endsWith("index_1.shtml")) {
                        respond(ByteReadChannel(ByteArray(0)), HttpStatusCode.NotFound)
                    } else {
                        respond(ByteReadChannel(mainHtml.toByteArray(cp1251)))
                    }
                }
            val result = repo(engine).getAuthor("/t/test/")
            assertTrue(result is SamlibResult.Success)
            val author = (result as SamlibResult.Success).data
            assertTrue(author.categories.any { it.title == "Раздел А" })
        }
}
