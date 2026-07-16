package ru.samlib.client.ui.comments

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.orbitmvi.orbit.test.test
import ru.samlib.client.data.parser.CommentsParser
import ru.samlib.client.data.remote.SamlibHttpClient
import ru.samlib.client.data.repository.DefaultAuthRepository
import ru.samlib.client.data.repository.DefaultCommentsRepository
import ru.samlib.client.domain.repository.AuthRepository
import ru.samlib.client.domain.usecase.PostCommentUseCase
import java.io.File
import java.io.IOException

class CommentsViewModelTest {
    private fun bytes(name: String): ByteArray = javaClass.getResourceAsStream("/fixtures/$name")!!.readBytes()

    private fun repo(name: String) =
        DefaultCommentsRepository(
            SamlibHttpClient(MockEngine { respond(ByteReadChannel(bytes(name))) }),
            CommentsParser(),
        )

    private fun repoFailing() =
        DefaultCommentsRepository(
            SamlibHttpClient(MockEngine { throw IOException("no net") }),
            CommentsParser(),
        )

    private fun auth(): AuthRepository {
        val file = File.createTempFile("auth", ".preferences_pb").apply { delete() }
        val ds = PreferenceDataStoreFactory.create(produceFile = { file })
        return DefaultAuthRepository(
            ds,
            SamlibHttpClient(MockEngine { respond(ByteReadChannel(ByteArray(0))) }),
        )
    }

    @Test
    fun `load наполняет комментарии`() =
        runTest {
            val commentsRepo = repo("comment_page.html")
            val vm =
                CommentsViewModel(
                    "/comment/p/prosto_o_w/news",
                    commentsRepo,
                    auth(),
                    PostCommentUseCase(commentsRepo),
                )
            vm.test(this) {
                expectInitialState()
                containerHost.load()
                assertEquals(true, awaitState().loading)
                val loaded = awaitState()
                assertEquals(false, loaded.loading)
                assertTrue(loaded.comments.isNotEmpty())
                assertEquals(null, loaded.error)
            }
        }

    @Test
    fun `load при ошибке выставляет error`() =
        runTest {
            val commentsRepo = repoFailing()
            val vm = CommentsViewModel("/comment/x", commentsRepo, auth(), PostCommentUseCase(commentsRepo))
            vm.test(this) {
                expectInitialState()
                containerHost.load()
                assertEquals(true, awaitState().loading)
                val failed = awaitState()
                assertTrue(failed.error != null)
            }
        }
}
