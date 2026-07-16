package ru.samlib.client.ui.home

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.orbitmvi.orbit.test.test
import ru.samlib.client.data.db.ObservedWorkEntity
import ru.samlib.client.data.parser.AuthorParser
import ru.samlib.client.data.parser.WorkParser
import ru.samlib.client.data.remote.SamlibHttpClient
import ru.samlib.client.data.repository.DefaultAuthorRepository
import ru.samlib.client.data.testfixtures.FakeAuthorDao
import ru.samlib.client.data.testfixtures.FakeObservedWorkDao
import ru.samlib.client.domain.model.Author
import ru.samlib.client.domain.usecase.ImportFollowedAuthorsUseCase
import ru.samlib.client.domain.usecase.UnfollowAuthorUseCase

class FollowedViewModelTest {
    private fun repo(observedDao: FakeObservedWorkDao = FakeObservedWorkDao()) =
        DefaultAuthorRepository(
            SamlibHttpClient(MockEngine { respond(ByteReadChannel(ByteArray(0))) }),
            AuthorParser(),
            FakeAuthorDao(),
            observedDao,
            WorkParser(),
        )

    @Test
    fun `observe отдаёт отслеживаемых, unfollow убирает`() =
        runTest {
            val authorRepo = repo()
            authorRepo.follow(Author(link = "/a/x/", fullName = "Иван"))
            val vm =
                FollowedViewModel(
                    authorRepo,
                    UnfollowAuthorUseCase(authorRepo),
                    ImportFollowedAuthorsUseCase(authorRepo),
                )
            vm.test(this) {
                expectInitialState()
                containerHost.observe()
                var s = awaitState()
                while (s.items.isEmpty()) s = awaitState()
                assertTrue(s.items.any { it.link == "/a/x/" })

                containerHost.unfollow("/a/x/")
                awaitSideEffect() // AuthorDeleted — для снек-бара с отменой
                while (s.items.isNotEmpty()) s = awaitState()
                assertTrue(s.items.isEmpty())

                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `удаление книги шлёт side effect, restore возвращает её в список`() =
        runTest {
            val observedDao = FakeObservedWorkDao()
            val authorRepo = repo(observedDao)
            observedDao.upsert(
                ObservedWorkEntity(
                    link = "/t/w.shtml",
                    authorLink = "/t/",
                    title = "Книга",
                    isStandalone = true,
                ),
            )
            val vm =
                FollowedViewModel(
                    authorRepo,
                    UnfollowAuthorUseCase(authorRepo),
                    ImportFollowedAuthorsUseCase(authorRepo),
                )
            vm.test(this) {
                expectInitialState()
                containerHost.observeBooks()
                var s = awaitState()
                while (s.books.isEmpty()) s = awaitState()

                containerHost.deleteBook(s.books.first().toDomain())
                val eff = awaitSideEffect() as FollowedSideEffect.BookDeleted
                assertEquals("/t/w.shtml", eff.work.link)
                while (s.books.isNotEmpty()) s = awaitState()

                containerHost.restoreBook(eff.work)
                while (s.books.isEmpty()) s = awaitState()
                assertTrue(s.books.any { it.link == "/t/w.shtml" })
                cancelAndIgnoreRemainingItems()
            }
        }
}
