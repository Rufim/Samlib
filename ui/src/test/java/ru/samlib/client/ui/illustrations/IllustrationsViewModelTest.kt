package ru.samlib.client.ui.illustrations

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.orbitmvi.orbit.test.test
import ru.samlib.client.data.parser.IllustrationsParser
import ru.samlib.client.data.remote.SamlibHttpClient
import ru.samlib.client.data.repository.DefaultIllustrationsRepository
import ru.samlib.client.domain.repository.IllustrationsRepository
import java.io.IOException

class IllustrationsViewModelTest {
    private fun fixtureBytes(name: String): ByteArray = javaClass.getResourceAsStream("/fixtures/$name")!!.readBytes()

    private fun repoWithFixture(name: String): IllustrationsRepository {
        val engine = MockEngine { respond(ByteReadChannel(fixtureBytes(name))) }
        return DefaultIllustrationsRepository(SamlibHttpClient(engine), IllustrationsParser())
    }

    private fun repoFailing(): IllustrationsRepository {
        val engine = MockEngine { throw IOException("no network") }
        return DefaultIllustrationsRepository(SamlibHttpClient(engine), IllustrationsParser())
    }

    @Test
    fun `load наполняет список из фикстуры`() =
        runTest {
            val vm = IllustrationsViewModel("/w/wrochek_s/deadgod.shtml", repoWithFixture("illustrations.html"))
            vm.test(this) {
                expectInitialState()
                containerHost.load()
                assertEquals(true, awaitState().loading)
                val loaded = awaitState()
                assertEquals(false, loaded.loading)
                assertTrue(loaded.images.isNotEmpty())
                assertEquals(null, loaded.error)
            }
        }

    @Test
    fun `повторный load не делает новый запрос`() =
        runTest {
            var requests = 0
            val engine =
                MockEngine {
                    requests++
                    respond(ByteReadChannel(fixtureBytes("illustrations.html")))
                }
            val vm =
                IllustrationsViewModel(
                    "/w/wrochek_s/deadgod.shtml",
                    DefaultIllustrationsRepository(SamlibHttpClient(engine), IllustrationsParser()),
                )
            vm.test(this) {
                expectInitialState()
                containerHost.load()
                awaitState() // loading = true
                awaitState() // загружено
                containerHost.load()
                assertEquals("повторный load не бьёт в сеть", 1, requests)
                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `load при ошибке сети выставляет error`() =
        runTest {
            val vm = IllustrationsViewModel("/w/wrochek_s/deadgod.shtml", repoFailing())
            vm.test(this) {
                expectInitialState()
                containerHost.load()
                assertEquals(true, awaitState().loading)
                val failed = awaitState()
                assertEquals(false, failed.loading)
                assertTrue(failed.images.isEmpty())
                assertTrue(failed.error != null)
            }
        }
}
