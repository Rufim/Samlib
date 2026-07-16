package ru.samlib.client.ui.author

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.orbitmvi.orbit.test.test
import ru.samlib.client.data.db.ObservedWorkEntity
import ru.samlib.client.data.parser.AuthorParser
import ru.samlib.client.data.parser.WorkParser
import ru.samlib.client.data.remote.SamlibHttpClient
import ru.samlib.client.data.repository.DefaultAuthRepository
import ru.samlib.client.data.repository.DefaultAuthorRepository
import ru.samlib.client.data.testfixtures.FakeAuthorDao
import ru.samlib.client.data.testfixtures.FakeObservedWorkDao
import ru.samlib.client.domain.repository.AuthRepository
import ru.samlib.client.domain.repository.AuthorRepository
import java.io.File
import java.io.IOException

class AuthorViewModelTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun fixtureBytes(name: String): ByteArray = javaClass.getResourceAsStream("/fixtures/$name")!!.readBytes()

    // isOwnPage не проверяется в этих тестах; нужен лишь конструируемый AuthRepository.
    private fun authRepo(): AuthRepository {
        val ds =
            PreferenceDataStoreFactory.create(
                produceFile = { File(tmp.newFolder(), "auth.preferences_pb") },
            )
        return DefaultAuthRepository(
            ds,
            SamlibHttpClient(MockEngine { respond(ByteReadChannel(ByteArray(0))) }),
        )
    }

    private fun repoWithFixture(name: String): AuthorRepository {
        val engine = MockEngine { respond(ByteReadChannel(fixtureBytes(name))) }
        return DefaultAuthorRepository(
            SamlibHttpClient(engine),
            AuthorParser(),
            FakeAuthorDao(),
            FakeObservedWorkDao(),
            WorkParser(),
        )
    }

    private fun repoFailing(): AuthorRepository {
        val engine = MockEngine { throw IOException("no network") }
        return DefaultAuthorRepository(
            SamlibHttpClient(engine),
            AuthorParser(),
            FakeAuthorDao(),
            FakeObservedWorkDao(),
            WorkParser(),
        )
    }

    @Test
    fun `load загружает автора и его работы`() =
        runTest {
            val vm = AuthorViewModel("/m/melxnik_a_a/", repoWithFixture("author.html"), authRepo())
            vm.test(this) {
                expectInitialState()
                containerHost.load()
                assertEquals(true, awaitState().loading)
                val loaded = awaitState()
                assertEquals(false, loaded.loading)
                assertTrue(loaded.author != null)
                assertTrue(loaded.author!!.works.isNotEmpty())
            }
        }

    @Test
    fun `load при ошибке выставляет error`() =
        runTest {
            val vm = AuthorViewModel("/m/melxnik_a_a/", repoFailing(), authRepo())
            vm.test(this) {
                expectInitialState()
                containerHost.load()
                assertEquals(true, awaitState().loading)
                val failed = awaitState()
                assertEquals(false, failed.loading)
                assertTrue(failed.author == null)
                assertTrue(failed.error != null)
            }
        }

    @Test
    fun `onWorkClick шлёт OpenWork`() =
        runTest {
            val vm = AuthorViewModel("/m/melxnik_a_a/", repoWithFixture("author.html"), authRepo())
            vm.test(this) {
                expectInitialState()
                containerHost.onWorkClick("/m/melxnik_a_a/letnijdozhdik.shtml")
                assertEquals(
                    AuthorSideEffect.OpenWork("/m/melxnik_a_a/letnijdozhdik.shtml"),
                    awaitSideEffect(),
                )
            }
        }

    @Test
    fun `toggleFollow помечает и снимает отслеживание`() =
        runTest {
            val vm = AuthorViewModel("/m/melxnik_a_a/", repoWithFixture("author.html"), authRepo())
            vm.test(this) {
                expectInitialState()
                containerHost.observeFollow()
                containerHost.toggleFollow()
                var s = awaitState()
                while (!s.isFollowed) s = awaitState()
                assertTrue(s.isFollowed)

                containerHost.toggleFollow()
                while (s.isFollowed) s = awaitState()
                assertTrue(!s.isFollowed)

                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `observeUpdates собирает изменённые книги и их разделы`() =
        runTest {
            val observedDao = FakeObservedWorkDao()
            observedDao.upsert(
                ObservedWorkEntity(
                    link = "/t/test/w1.shtml",
                    authorLink = "/t/test/",
                    categoryTitle = "Раздел А",
                    changed = true,
                    sizeDiff = 2,
                ),
            )
            val repo =
                DefaultAuthorRepository(
                    SamlibHttpClient(
                        MockEngine { respond(ByteReadChannel(ByteArray(0))) },
                    ),
                    AuthorParser(),
                    FakeAuthorDao(),
                    observedDao,
                    WorkParser(),
                )
            val vm = AuthorViewModel("/t/test/", repo, authRepo())
            vm.test(this) {
                expectInitialState()
                containerHost.observeUpdates()
                var s = awaitState()
                while (s.changedLinks.isEmpty()) s = awaitState()
                assertTrue("/t/test/w1.shtml" in s.changedLinks)
                assertTrue("Раздел А" in s.changedCategories)
                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `toggleSection переключает свёрнутость`() =
        runTest {
            val vm = AuthorViewModel("/m/melxnik_a_a/", repoWithFixture("author.html"), authRepo())
            vm.test(this) {
                expectInitialState()
                containerHost.toggleSection("Стихотворение")
                assertTrue("Стихотворение" in awaitState().collapsed)
                containerHost.toggleSection("Стихотворение")
                assertTrue("Стихотворение" !in awaitState().collapsed)
                cancelAndIgnoreRemainingItems()
            }
        }
}
