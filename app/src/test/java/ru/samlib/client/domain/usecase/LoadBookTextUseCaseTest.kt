package ru.samlib.client.domain.usecase

import androidx.test.core.app.ApplicationProvider
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.samlib.client.data.parser.DefaultWorkContentParser
import ru.samlib.client.data.parser.WorkParser
import ru.samlib.client.data.remote.SamlibHttpClient
import ru.samlib.client.data.repository.DefaultWorkRepository
import ru.samlib.client.data.testfixtures.FakeBookmarkDao
import ru.samlib.client.data.testfixtures.FakeCachedWorkDao
import ru.samlib.client.data.testfixtures.FakeHistoryDao
import ru.samlib.client.domain.model.ContentBlock
import ru.samlib.client.domain.repository.LocalDocument
import ru.samlib.client.domain.repository.LocalFileRepository
import ru.samlib.client.ui.work.ComposeBlockTextExtractor
import java.nio.charset.Charset

private class FakeLocalRepo(
    private val doc: LocalDocument?,
) : LocalFileRepository {
    override suspend fun load(uri: String): LocalDocument = doc ?: error("no doc")

    override fun hasPersistedAccess(uri: String): Boolean = false

    override suspend fun ensureLocalCopy(uri: String): String = uri

    override fun persistReadAccess(uri: String) = Unit
}

@RunWith(RobolectricTestRunner::class)
class LoadBookTextUseCaseTest {
    private fun workRepo(html: String) =
        DefaultWorkRepository(
            ApplicationProvider.getApplicationContext(),
            SamlibHttpClient(
                MockEngine { respond(ByteReadChannel(html.toByteArray(Charset.forName("windows-1251")))) },
            ),
            WorkParser(),
            FakeHistoryDao(),
            FakeBookmarkDao(),
            FakeCachedWorkDao(),
        )

    @Test
    fun `link грузит текст из сети и строит индекс`() =
        runTest {
            val html = "<html><body><h2>T</h2><pre>Привет мир</pre></body></html>"
            val useCase =
                LoadBookTextUseCase(
                    workRepo(html),
                    DefaultWorkContentParser(),
                    FakeLocalRepo(null),
                    ComposeBlockTextExtractor(),
                )
            val result = useCase(LoadBookTextUseCase.Params(link = "/a/b/text.shtml", uri = null))
            assertTrue(result.texts.any { it.contains("Привет") })
            assertEquals(result.texts.map { it.lowercase() }, result.textsLower)
        }

    @Test
    fun `uri грузит текст из локального файла`() =
        runTest {
            val doc = LocalDocument("f.txt", listOf(ContentBlock.Preformatted("Локальный текст")))
            val useCase =
                LoadBookTextUseCase(
                    workRepo(""),
                    DefaultWorkContentParser(),
                    FakeLocalRepo(doc),
                    ComposeBlockTextExtractor(),
                )
            val result = useCase(LoadBookTextUseCase.Params(link = null, uri = "file://f.txt"))
            assertEquals(listOf("Локальный текст"), result.texts)
        }

    @Test
    fun `ни link ни uri — пустой результат`() =
        runTest {
            val useCase =
                LoadBookTextUseCase(
                    workRepo(""),
                    DefaultWorkContentParser(),
                    FakeLocalRepo(null),
                    ComposeBlockTextExtractor(),
                )
            val result = useCase(LoadBookTextUseCase.Params(link = null, uri = null))
            assertTrue(result.texts.isEmpty())
        }
}
