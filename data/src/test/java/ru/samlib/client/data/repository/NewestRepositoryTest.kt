package ru.samlib.client.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.samlib.client.data.parser.NewestParser
import ru.samlib.client.data.remote.SamlibHttpClient
import ru.samlib.client.domain.model.WorkFilter
import ru.samlib.client.domain.repository.PageRequest
import ru.samlib.client.domain.repository.SamlibResult

class NewestRepositoryTest {
    private fun bytes() = javaClass.getResourceAsStream("/fixtures/newest_long.html")!!.readBytes()

    @Test
    fun `второй getNewest без refresh не делает повторный запрос`() =
        runTest {
            var requests = 0
            val engine =
                MockEngine {
                    requests++
                    respond(ByteReadChannel(bytes()))
                }
            val repo = DefaultNewestRepository(SamlibHttpClient(engine), NewestParser())
            val filter = WorkFilter()

            val p1 = repo.getNewest(PageRequest(0, 50), filter)
            val p2 = repo.getNewest(PageRequest(50, 50), filter)
            assertTrue(p1 is SamlibResult.Success && p2 is SamlibResult.Success)
            assertEquals("кеш: один сетевой запрос", 1, requests)

            repo.getNewest(PageRequest(0, 50), filter, refresh = true)
            assertEquals("refresh перечитывает", 2, requests)
        }

    @Test
    fun `слайсинг skip и size корректен`() =
        runTest {
            val filter = WorkFilter()
            val engine = MockEngine { respond(ByteReadChannel(bytes())) }
            val repo = DefaultNewestRepository(SamlibHttpClient(engine), NewestParser())
            val first = (repo.getNewest(PageRequest(0, 10), filter) as SamlibResult.Success).data
            val second = (repo.getNewest(PageRequest(10, 10), filter) as SamlibResult.Success).data
            assertEquals(10, first.size)
            assertTrue(second.isNotEmpty())
            assertTrue("разные страницы", first.first().link != second.first().link)
        }
}
