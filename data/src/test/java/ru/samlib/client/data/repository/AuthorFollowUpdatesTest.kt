package ru.samlib.client.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.samlib.client.data.db.ObservedWorkEntity
import ru.samlib.client.data.parser.AuthorParser
import ru.samlib.client.data.parser.WorkParser
import ru.samlib.client.data.remote.SamlibHttpClient
import ru.samlib.client.data.testfixtures.FakeAuthorDao
import ru.samlib.client.data.testfixtures.FakeObservedWorkDao
import ru.samlib.client.domain.model.Author
import java.nio.charset.Charset

class AuthorFollowUpdatesTest {
    private val cp1251: Charset = Charset.forName("windows-1251")

    private fun authorHtml(size: String) =
        """
        <html><body><center><h3>Тест Автор</h3></center>
        <!-- ------- Блок ссылок на произведения ---------- -->
        <DL><DT><li><A HREF=work1.shtml><b>Работа 1</b></A> &nbsp; <b>$size</b> &nbsp; <small> Проза </small></DL>
        <!-- ------- Подножие ----------------------------- -->
        </body></html>
        """.trimIndent()

    @Test
    fun `первая проверка базовая, вторая помечает изменение размера, markSeen сбрасывает`() =
        runTest {
            var size = "2k"
            val engine =
                MockEngine { respond(ByteReadChannel(authorHtml(size).toByteArray(cp1251))) }
            val authorDao = FakeAuthorDao()
            val observedDao = FakeObservedWorkDao()
            val repo =
                DefaultAuthorRepository(
                    SamlibHttpClient(engine),
                    AuthorParser(),
                    authorDao,
                    observedDao,
                    WorkParser(),
                )

            repo.follow(Author(link = "/t/test/"))
            assertTrue(
                "первая проверка — baseline без пометок",
                repo.checkFollowedUpdates().isEmpty(),
            )
            assertEquals(1, observedDao.getByAuthor("/t/test/").size)

            size = "5k"
            val changed = repo.checkFollowedUpdates()
            assertEquals(1, changed.size)
            assertEquals(3, changed.first().sizeDiff)
            assertTrue(changed.first().changed)
            assertTrue(authorDao.get("/t/test/")!!.hasUpdates)
            assertTrue(authorDao.get("/t/test/")!!.notNotified)

            // повторная проверка без изменений — не дублирует уведомление
            assertTrue(repo.checkFollowedUpdates().isEmpty())

            repo.markSeen("/t/test/")
            assertFalse(authorDao.get("/t/test/")!!.hasUpdates)
            assertTrue(observedDao.getByAuthor("/t/test/").none { it.changed })
        }

    @Test
    fun `markWorkOpened сбрасывает книгу и флаг автора когда изменённых не осталось`() =
        runTest {
            val authorDao = FakeAuthorDao()
            val observedDao = FakeObservedWorkDao()
            val engine = MockEngine { respond(ByteReadChannel(ByteArray(0))) }
            val repo =
                DefaultAuthorRepository(
                    SamlibHttpClient(engine),
                    AuthorParser(),
                    authorDao,
                    observedDao,
                    WorkParser(),
                )
            repo.follow(Author(link = "/t/test/"))
            authorDao.setUpdateFlags("/t/test/", hasUpdates = true, notNotified = false)
            observedDao.upsert(
                ObservedWorkEntity(
                    link = "/t/test/w1.shtml",
                    authorLink = "/t/test/",
                    changed = true,
                    sizeDiff = 2,
                ),
            )
            repo.markWorkOpened("/t/test/w1.shtml")
            assertFalse(observedDao.get("/t/test/w1.shtml")!!.changed)
            assertFalse(authorDao.get("/t/test/")!!.hasUpdates)
        }
}
