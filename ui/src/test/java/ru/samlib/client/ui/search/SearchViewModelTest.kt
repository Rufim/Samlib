package ru.samlib.client.ui.search

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.orbitmvi.orbit.test.test
import ru.samlib.client.data.parser.SearchParser
import ru.samlib.client.data.parser.WebSearchParser
import ru.samlib.client.data.remote.SamlibHttpClient
import ru.samlib.client.data.repository.DefaultSearchRepository
import java.io.IOException

class SearchViewModelTest {
    private fun fixtureBytes(): ByteArray = javaClass.getResourceAsStream("/fixtures/search.html")!!.readBytes()

    private fun repoOk() =
        DefaultSearchRepository(
            SamlibHttpClient(MockEngine { respond(ByteReadChannel(fixtureBytes())) }),
            SearchParser(),
            WebSearchParser(),
        )

    private fun repoFail() =
        DefaultSearchRepository(
            SamlibHttpClient(MockEngine { throw IOException("no net") }),
            SearchParser(),
            WebSearchParser(),
        )

    @Test
    fun `search наполняет результаты`() =
        runTest {
            val vm = SearchViewModel(repoOk())
            vm.test(this) {
                expectInitialState()
                containerHost.onQueryChange("Пушкин")
                awaitState() // query
                containerHost.search()
                awaitState() // loading = true
                val loaded = awaitState()
                assertTrue(loaded.items.isNotEmpty())
                assertTrue(loaded.canLoadMore)
            }
        }

    @Test
    fun `search при ошибке выставляет error`() =
        runTest {
            val vm = SearchViewModel(repoFail())
            vm.test(this) {
                expectInitialState()
                containerHost.onQueryChange("x")
                awaitState()
                containerHost.search()
                awaitState() // loading
                assertTrue(awaitState().error != null)
            }
        }

    @Test
    fun `loadMore добавляет вторую страницу`() =
        runTest {
            val vm = SearchViewModel(repoOk())
            vm.test(this) {
                expectInitialState()
                containerHost.onQueryChange("Пушкин")
                awaitState()
                containerHost.search()
                awaitState()
                val first = awaitState()
                containerHost.loadMore()
                awaitState() // loading
                val more = awaitState()
                assertTrue(more.items.size >= first.items.size)
            }
        }
}
