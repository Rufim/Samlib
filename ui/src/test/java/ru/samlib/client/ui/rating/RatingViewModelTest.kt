package ru.samlib.client.ui.rating

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.orbitmvi.orbit.test.test
import ru.samlib.client.data.parser.DiscussionParser
import ru.samlib.client.data.parser.TopAuthorsParser
import ru.samlib.client.data.parser.WorkRatingParser
import ru.samlib.client.data.remote.SamlibHttpClient
import ru.samlib.client.data.repository.DefaultRatingRepository
import ru.samlib.client.domain.model.RatingSection
import ru.samlib.client.domain.repository.RatingRepository
import java.io.IOException

class RatingViewModelTest {
    private fun fixtureBytes(name: String): ByteArray = javaClass.getResourceAsStream("/fixtures/$name")!!.readBytes()

    private fun repoWithFixture(name: String): RatingRepository {
        val engine = MockEngine { respond(ByteReadChannel(fixtureBytes(name))) }
        return DefaultRatingRepository(
            SamlibHttpClient(engine),
            TopAuthorsParser(),
            WorkRatingParser(),
            DiscussionParser(),
        )
    }

    private fun repoFailing(): RatingRepository {
        val engine = MockEngine { throw IOException("no network") }
        return DefaultRatingRepository(
            SamlibHttpClient(engine),
            TopAuthorsParser(),
            WorkRatingParser(),
            DiscussionParser(),
        )
    }

    @Test
    fun `load наполняет список авторов`() =
        runTest {
            val vm = RatingViewModel(RatingSection.TOP_AUTHORS, repoWithFixture("rating_hits.html"))
            vm.test(this) {
                expectInitialState()
                containerHost.load()
                assertEquals(true, awaitState().loading)
                val loaded = awaitState()
                assertEquals(false, loaded.loading)
                assertTrue(loaded.rows.isNotEmpty())
                assertEquals(null, loaded.error)
            }
        }

    @Test
    fun `load при ошибке выставляет error`() =
        runTest {
            val vm = RatingViewModel(RatingSection.DISCUSSIONS, repoFailing())
            vm.test(this) {
                expectInitialState()
                containerHost.load()
                assertEquals(true, awaitState().loading)
                val failed = awaitState()
                assertEquals(false, failed.loading)
                assertTrue(failed.rows.isEmpty())
                assertTrue(failed.error != null)
            }
        }

    @Test
    fun `onWorkClick шлёт side-effect навигации`() =
        runTest {
            val vm = RatingViewModel(RatingSection.FAVORITES, repoWithFixture("rating_expert.html"))
            vm.test(this) {
                expectInitialState()
                containerHost.onWorkClick("/w/wrochek_s/deadgod.shtml")
                assertEquals(
                    RatingSideEffect.NavigateToWork("/w/wrochek_s/deadgod.shtml"),
                    awaitSideEffect(),
                )
            }
        }
}
