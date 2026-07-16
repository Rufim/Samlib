package ru.samlib.client.data.repository

import androidx.test.core.app.ApplicationProvider
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.samlib.client.data.db.CachedWorkEntity
import ru.samlib.client.data.parser.WorkParser
import ru.samlib.client.data.remote.SamlibHttpClient
import ru.samlib.client.data.testfixtures.FakeBookmarkDao
import ru.samlib.client.data.testfixtures.FakeCachedWorkDao
import ru.samlib.client.data.testfixtures.FakeHistoryDao
import ru.samlib.client.domain.model.Work
import ru.samlib.client.domain.repository.SamlibResult
import java.io.IOException
import java.nio.charset.Charset

@RunWith(RobolectricTestRunner::class)
class WorkRepositoryCacheTest {
    private val cp1251: Charset = Charset.forName("windows-1251")
    private val link = "/a/author/kniga.shtml"

    private fun workHtml(body: String) =
        """
        <html><head><title>Автор Тест. Книга</title></head><body>
        <!----------- Собственно произведение --------------->
        $body
        <!--------- Блок описания произведения (слева внизу) ----------------------->
        </body></html>
        """.trimIndent()

    /** MockEngine с переключателем online/offline (offline бросает IOException). */
    private class ToggleEngine(
        val htmlProvider: () -> String,
    ) {
        var online = true
        val engine =
            MockEngine {
                if (online) {
                    respond(ByteReadChannel(htmlProvider().toByteArray(Charset.forName("windows-1251"))))
                } else {
                    throw IOException("offline")
                }
            }
    }

    private fun repo(engine: MockEngine) =
        DefaultWorkRepository(
            ApplicationProvider.getApplicationContext(),
            SamlibHttpClient(engine),
            WorkParser(),
            FakeHistoryDao(),
            FakeBookmarkDao(),
            FakeCachedWorkDao(),
        )

    @Test
    fun `офлайн-чтение отдаёт кешированный текст после онлайн-открытия`() =
        runTest {
            val toggle = ToggleEngine { workHtml("<p>Тело произведения для офлайна.</p>") }
            val repo = repo(toggle.engine)

            val online = repo.getWork(link)
            assertTrue(online is SamlibResult.Success)
            val onlineText = (online as SamlibResult.Success).data.rawContent
            assertTrue(onlineText.contains("Тело произведения"))

            toggle.online = false
            val offline = repo.getWork(link)
            assertTrue("офлайн должен отдать кеш", offline is SamlibResult.Success)
            assertEquals(onlineText, (offline as SamlibResult.Success).data.rawContent)
            assertEquals("Книга", offline.data.title)
        }

    @Test
    fun `офлайн без кеша возвращает сетевую ошибку`() =
        runTest {
            val engine = MockEngine { throw IOException("offline") }
            val res = repo(engine).getWork("/a/author/none.shtml")
            assertTrue(res is SamlibResult.Error)
        }

    @Test
    fun `большая книга больше 2МБ кешируется и читается офлайн`() =
        runTest {
            // Регрессия на CursorWindow: в БД строка > ~2 МБ не читалась. Файловый кеш должен справиться.
            val big = "Строка текста произведения. ".repeat(120_000) // ~3.3 МБ
            val toggle = ToggleEngine { workHtml("<p>$big</p>") }
            val repo = repo(toggle.engine)

            val online = repo.getWork(link)
            assertTrue(online is SamlibResult.Success)
            assertTrue((online as SamlibResult.Success).data.rawContent.length > 2_000_000)

            toggle.online = false
            val offline = repo.getWork(link)
            assertTrue(offline is SamlibResult.Success)
            assertTrue(
                "офлайн-текст крупной книги должен восстановиться из файла",
                (offline as SamlibResult.Success).data.rawContent.length > 2_000_000,
            )
        }

    @Test
    fun `hasCached true после кеша и false без него`() =
        runTest {
            val toggle = ToggleEngine { workHtml("<p>Текст.</p>") }
            val repo = repo(toggle.engine)
            assertFalse(repo.hasCached(link))
            repo.getWork(link)
            assertTrue(repo.hasCached(link))
        }

    @Test
    fun `clearWorkCache удаляет файлы и записи`() =
        runTest {
            val toggle = ToggleEngine { workHtml("<p>Текст.</p>") }
            val repo = repo(toggle.engine)
            repo.getWork(link)
            assertTrue(repo.hasCached(link))

            repo.clearWorkCache()
            assertFalse(repo.hasCached(link))
            toggle.online = false
            assertTrue(
                "после очистки офлайн-чтение недоступно",
                repo.getWork(link) is SamlibResult.Error,
            )
        }

    @Test
    fun `офлайн с записью в БД но без файла возвращает ошибку`() =
        runTest {
            val cachedDao = FakeCachedWorkDao()
            val engine = MockEngine { throw IOException("offline") }
            val repo =
                DefaultWorkRepository(
                    ApplicationProvider.getApplicationContext(),
                    SamlibHttpClient(engine),
                    WorkParser(),
                    FakeHistoryDao(),
                    FakeBookmarkDao(),
                    cachedDao,
                )
            // запись есть, а файл по пути отсутствует
            cachedDao.upsert(
                CachedWorkEntity(
                    link = link,
                    title = "Книга",
                    authorLink = null,
                    authorShortName = null,
                    categoryTitle = null,
                    imageLink = null,
                    size = null,
                    sizeDiff = null,
                    rate = null,
                    votes = null,
                    expertRate = null,
                    expertVotes = null,
                    commentCount = null,
                    customGenres = null,
                    type = "OTHER",
                    annotation = null,
                    description = null,
                    createDate = null,
                    updateDate = null,
                    dateText = null,
                    changedDate = null,
                    genres = emptyList(),
                    annotationBlocks = emptyList(),
                    filePath = "/nonexistent/path/x.html",
                    savedDate = 1,
                ),
            )
            assertTrue(repo.getWork(link) is SamlibResult.Error)
        }

    @Test
    fun `addToHistory не создаёт файл кеша`() =
        runTest {
            val engine = MockEngine { throw IOException("offline") }
            val repo = repo(engine)
            repo.addToHistory(
                Work(link = link, title = "Книга", rawContent = "тело"),
            )
            assertFalse("кеш пишет только getWork при загрузке из сети", repo.hasCached(link))
        }
}
