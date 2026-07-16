package ru.samlib.client.ui.work

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.orbitmvi.orbit.test.test
import org.robolectric.RobolectricTestRunner
import ru.samlib.client.data.local.DefaultReaderFileGateway
import ru.samlib.client.data.parser.WorkParser
import ru.samlib.client.data.remote.SamlibHttpClient
import ru.samlib.client.data.repository.DefaultWorkRepository
import ru.samlib.client.data.settings.DefaultReaderSettingsRepository
import ru.samlib.client.data.testfixtures.FakeBookmarkDao
import ru.samlib.client.data.testfixtures.FakeCachedWorkDao
import ru.samlib.client.data.testfixtures.FakeHistoryDao
import ru.samlib.client.data.translate.DefaultTranslator
import ru.samlib.client.domain.model.ContentBlock
import ru.samlib.client.domain.model.SavedItem
import ru.samlib.client.domain.repository.LocalDocument
import ru.samlib.client.domain.repository.LocalFileRepository
import ru.samlib.client.domain.repository.SavedBookInfo
import ru.samlib.client.domain.repository.SavedBooksRepository
import ru.samlib.client.domain.usecase.OpenExternallyUseCase
import java.io.File

private class FakeLocalRepo(
    val doc: LocalDocument?,
    val fail: Boolean = false,
) : LocalFileRepository {
    override suspend fun load(uri: String): LocalDocument {
        if (fail) throw IllegalStateException("boom")
        return doc!!
    }

    override fun hasPersistedAccess(uri: String): Boolean = false

    override suspend fun ensureLocalCopy(uri: String): String = uri

    override fun persistReadAccess(uri: String) = Unit
}

private class FakeSavedBooksRepo : SavedBooksRepository {
    override suspend fun all(): List<SavedItem> = emptyList()

    override suspend fun record(info: SavedBookInfo) = Unit

    override suspend fun delete(uriString: String) = Unit
}

@RunWith(RobolectricTestRunner::class)
class LocalWorkViewModelTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun settingsRepo() =
        DefaultReaderSettingsRepository(
            PreferenceDataStoreFactory.create(produceFile = {
                File(
                    tmp.newFolder(),
                    "r.preferences_pb",
                )
            }),
        )

    private fun workRepo() =
        DefaultWorkRepository(
            ApplicationProvider.getApplicationContext(),
            SamlibHttpClient(MockEngine { respond(ByteReadChannel(ByteArray(0))) }),
            WorkParser(),
            FakeHistoryDao(),
            FakeBookmarkDao(),
            FakeCachedWorkDao(),
        )

    @Test
    fun `load наполняет блоки из локального документа`() =
        runTest {
            val doc = LocalDocument("f.txt", listOf(ContentBlock.Paragraph("Привет")))
            val vm =
                LocalWorkViewModel(
                    "file://f.txt",
                    -1,
                    FakeLocalRepo(doc),
                    workRepo(),
                    settingsRepo(),
                    FakeSavedBooksRepo(),
                    DefaultTranslator(),
                    OpenExternallyUseCase(DefaultReaderFileGateway(ApplicationProvider.getApplicationContext())),
                    InBookSearchMemory(),
                    ttsServiceLauncher = TtsServiceLauncher { },
                    player = FakeTtsPlaybackControls(),
                )
            vm.test(this) {
                expectInitialState()
                containerHost.load()
                skipItems(1) // loading = true
                val loaded = awaitState()
                assertEquals(false, loaded.loading)
                assertEquals("f.txt", loaded.title)
                assertTrue(loaded.blocks.isNotEmpty())
            }
        }

    @Test
    fun `togglePin закрепляет локальную книгу в истории`() =
        runTest {
            val doc = LocalDocument("f.txt", listOf(ContentBlock.Paragraph("Привет")))
            val repo = workRepo()
            val vm =
                LocalWorkViewModel(
                    "file://f.txt",
                    -1,
                    FakeLocalRepo(doc),
                    repo,
                    settingsRepo(),
                    FakeSavedBooksRepo(),
                    DefaultTranslator(),
                    OpenExternallyUseCase(DefaultReaderFileGateway(ApplicationProvider.getApplicationContext())),
                    InBookSearchMemory(),
                    ttsServiceLauncher = TtsServiceLauncher { },
                    player = FakeTtsPlaybackControls(),
                )
            vm.test(this) {
                expectInitialState()
                containerHost.load()
                skipItems(1) // loading = true
                val loaded = awaitState()
                assertEquals(false, loaded.pinned)
                containerHost.togglePin()
                assertTrue(awaitState().pinned)
                assertEquals(true, repo.isHistoryPinned("file://f.txt"))
            }
        }

    @Test
    fun `load при ошибке выставляет error`() =
        runTest {
            val vm =
                LocalWorkViewModel(
                    "file://x",
                    -1,
                    FakeLocalRepo(null, fail = true),
                    workRepo(),
                    settingsRepo(),
                    FakeSavedBooksRepo(),
                    DefaultTranslator(),
                    OpenExternallyUseCase(DefaultReaderFileGateway(ApplicationProvider.getApplicationContext())),
                    InBookSearchMemory(),
                    ttsServiceLauncher = TtsServiceLauncher { },
                    player = FakeTtsPlaybackControls(),
                )
            vm.test(this) {
                expectInitialState()
                containerHost.load()
                skipItems(1)
                assertTrue(awaitState().error != null)
            }
        }
}
