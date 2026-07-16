package ru.samlib.client.ui.newest

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.orbitmvi.orbit.test.test
import ru.samlib.client.data.parser.NewestParser
import ru.samlib.client.data.remote.SamlibHttpClient
import ru.samlib.client.data.repository.DefaultNewestRepository
import ru.samlib.client.domain.repository.NewestRepository
import java.io.IOException

class NewestViewModelTest {
    private fun fixtureBytes(name: String): ByteArray = javaClass.getResourceAsStream("/fixtures/$name")!!.readBytes()

    private fun repoWithFixture(name: String): NewestRepository {
        val engine = MockEngine { respond(ByteReadChannel(fixtureBytes(name))) }
        return DefaultNewestRepository(SamlibHttpClient(engine), NewestParser())
    }

    private fun repoFailing(): NewestRepository {
        val engine = MockEngine { throw IOException("no network") }
        return DefaultNewestRepository(SamlibHttpClient(engine), NewestParser())
    }

    @Test
    fun `load наполняет список из фикстуры`() =
        runTest {
            val vm = NewestViewModel(repoWithFixture("newest_long.html"))
            vm.test(this) {
                expectInitialState()
                containerHost.load()
                assertEquals(true, awaitState().loading)
                val loaded = awaitState()
                assertEquals(false, loaded.loading)
                assertTrue(loaded.items.isNotEmpty())
                assertEquals(null, loaded.error)
            }
        }

    @Test
    fun `load при ошибке сети выставляет error`() =
        runTest {
            val vm = NewestViewModel(repoFailing())
            vm.test(this) {
                expectInitialState()
                containerHost.load()
                assertEquals(true, awaitState().loading)
                val failed = awaitState()
                assertEquals(false, failed.loading)
                assertTrue(failed.items.isEmpty())
                assertTrue(failed.error != null)
            }
        }

    @Test
    fun `loadMore добавляет следующую страницу`() =
        runTest {
            val vm = NewestViewModel(repoWithFixture("newest_long.html"))
            vm.test(this) {
                expectInitialState()
                containerHost.load()
                awaitState() // loading = true
                val loaded = awaitState()
                assertTrue(loaded.items.isNotEmpty())
                if (loaded.canLoadMore) {
                    containerHost.loadMore()
                    awaitState() // loadingMore = true
                    val more = awaitState()
                    assertTrue(more.items.size > loaded.items.size)
                }
                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `onAuthorClick шлёт side-effect навигации`() =
        runTest {
            val vm = NewestViewModel(repoWithFixture("newest_long.html"))
            vm.test(this) {
                expectInitialState()
                containerHost.onAuthorClick("/m/melxnik_a_a/")
                assertEquals(
                    NewestSideEffect.NavigateToAuthor("/m/melxnik_a_a/"),
                    awaitSideEffect(),
                )
            }
        }
}
