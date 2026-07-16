package ru.samlib.client.ui.home

import androidx.test.core.app.ApplicationProvider
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.orbitmvi.orbit.test.test
import org.robolectric.RobolectricTestRunner
import ru.samlib.client.data.parser.WorkParser
import ru.samlib.client.data.remote.SamlibHttpClient
import ru.samlib.client.data.repository.DefaultWorkRepository
import ru.samlib.client.data.testfixtures.FakeBookmarkDao
import ru.samlib.client.data.testfixtures.FakeCachedWorkDao
import ru.samlib.client.data.testfixtures.FakeHistoryDao
import ru.samlib.client.domain.model.Work

@RunWith(RobolectricTestRunner::class)
class HistoryViewModelTest {
    private fun repo() =
        DefaultWorkRepository(
            ApplicationProvider.getApplicationContext(),
            SamlibHttpClient(MockEngine { respond(ByteReadChannel(ByteArray(0))) }),
            WorkParser(),
            FakeHistoryDao(),
            FakeBookmarkDao(),
            FakeCachedWorkDao(),
        )

    @Test
    fun `observe отдаёт историю`() =
        runTest {
            val workRepo = repo()
            workRepo.addToHistory(Work(link = "/w/1", title = "Заголовок", authorShortName = "Автор"))
            val vm = HistoryViewModel(workRepo)
            vm.test(this) {
                expectInitialState()
                containerHost.observe()
                val s = awaitState()
                assertTrue(s.items.any { it.link == "/w/1" })
                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `поиск фильтрует по названию и автору`() =
        runTest {
            val workRepo = repo()
            workRepo.addToHistory(Work(link = "/a", title = "Мышь", authorShortName = "Иванов"))
            workRepo.addToHistory(Work(link = "/b", title = "Кот", authorShortName = "Петров"))
            val vm = HistoryViewModel(workRepo)
            vm.test(this) {
                expectInitialState()
                containerHost.observe()
                awaitState()
                containerHost.setQuery("кот")
                val s = awaitState()
                assertEquals(listOf("/b"), s.items.map { it.link })
                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `delete шлёт Deleted и restore возвращает запись`() =
        runTest {
            val workRepo = repo()
            workRepo.addToHistory(Work(link = "/a", title = "Мышь", authorShortName = "Иванов"))
            val vm = HistoryViewModel(workRepo)
            vm.test(this) {
                expectInitialState()
                containerHost.observe()
                awaitState()
                containerHost.delete("/a")
                val effect = awaitSideEffect()
                assertTrue(effect is HistorySideEffect.Deleted)
                assertEquals("/a", (effect as HistorySideEffect.Deleted).work.link)
                var s = awaitState()
                while (s.items.isNotEmpty()) s = awaitState()
                containerHost.restore(effect.work)
                var s2 = awaitState()
                while (s2.items.isEmpty()) s2 = awaitState()
                assertEquals(listOf("/a"), s2.items.map { it.link })
                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `clear оставляет закреплённые`() =
        runTest {
            val workRepo = repo()
            workRepo.addToHistory(Work(link = "/a", title = "A", authorShortName = "X"))
            workRepo.addToHistory(Work(link = "/c", title = "C", authorShortName = "Y"))
            workRepo.setHistoryPinned("/c", true)
            val vm = HistoryViewModel(workRepo)
            vm.test(this) {
                expectInitialState()
                containerHost.observe()
                var s = awaitState()
                while (s.items.size != 2) s = awaitState()
                containerHost.clear()
                while (s.items.size > 1) s = awaitState()
                assertEquals(listOf("/c"), s.items.map { it.link })
                cancelAndIgnoreRemainingItems()
            }
        }
}
