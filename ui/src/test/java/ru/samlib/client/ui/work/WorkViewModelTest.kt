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
import ru.samlib.client.data.parser.AuthorParser
import ru.samlib.client.data.parser.DefaultWorkContentParser
import ru.samlib.client.data.parser.WorkParser
import ru.samlib.client.data.remote.DefaultFileDownloadGateway
import ru.samlib.client.data.remote.SamlibHttpClient
import ru.samlib.client.data.repository.DefaultAuthorRepository
import ru.samlib.client.data.repository.DefaultWorkRepository
import ru.samlib.client.data.settings.DefaultReaderSettingsRepository
import ru.samlib.client.data.testfixtures.FakeAuthorDao
import ru.samlib.client.data.testfixtures.FakeBookmarkDao
import ru.samlib.client.data.testfixtures.FakeCachedWorkDao
import ru.samlib.client.data.testfixtures.FakeHistoryDao
import ru.samlib.client.data.testfixtures.FakeObservedWorkDao
import ru.samlib.client.data.translate.DefaultTranslator
import ru.samlib.client.domain.usecase.DownloadFb2UseCase
import ru.samlib.client.domain.usecase.LoadWorkUseCase
import ru.samlib.client.domain.usecase.OpenExternallyUseCase
import ru.samlib.client.domain.usecase.SaveWorkUseCase
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class WorkViewModelTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun fixtureBytes(name: String): ByteArray = javaClass.getResourceAsStream("/fixtures/$name")!!.readBytes()

    private fun settingsRepo() =
        DefaultReaderSettingsRepository(
            PreferenceDataStoreFactory.create(produceFile = {
                File(
                    tmp.newFolder(),
                    "r.preferences_pb",
                )
            }),
        )

    private fun repo(engine: MockEngine) =
        DefaultWorkRepository(
            ApplicationProvider.getApplicationContext(),
            SamlibHttpClient(engine),
            WorkParser(),
            FakeHistoryDao(),
            FakeBookmarkDao(),
            FakeCachedWorkDao(),
        )

    private fun authorRepo() =
        DefaultAuthorRepository(
            SamlibHttpClient(MockEngine { respond(ByteReadChannel(ByteArray(0))) }),
            AuthorParser(),
            FakeAuthorDao(),
            FakeObservedWorkDao(),
            WorkParser(),
        )

    @Test
    fun `load разбирает произведение на блоки`() =
        runTest {
            val engine = MockEngine { respond(ByteReadChannel(fixtureBytes("work.html"))) }
            val workRepo = repo(engine)
            val authors = authorRepo()
            val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
            val vm =
                WorkViewModel(
                    "/m/melxnik_a_a/letnijdozhdik.shtml",
                    -1,
                    workRepo,
                    settingsRepo(),
                    DefaultTranslator(),
                    authors,
                    LoadWorkUseCase(workRepo, authors, DefaultWorkContentParser()),
                    SaveWorkUseCase(DefaultReaderFileGateway(ctx)),
                    OpenExternallyUseCase(DefaultReaderFileGateway(ctx)),
                    DownloadFb2UseCase(DefaultFileDownloadGateway(ctx)),
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
                assertTrue(loaded.blocks.isNotEmpty())
            }
        }

    @Test
    fun `load при ошибке выставляет error`() =
        runTest {
            val engine = MockEngine { throw IOException("no net") }
            val workRepo = repo(engine)
            val authors = authorRepo()
            val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
            val vm =
                WorkViewModel(
                    "/x",
                    -1,
                    workRepo,
                    settingsRepo(),
                    DefaultTranslator(),
                    authors,
                    LoadWorkUseCase(workRepo, authors, DefaultWorkContentParser()),
                    SaveWorkUseCase(DefaultReaderFileGateway(ctx)),
                    OpenExternallyUseCase(DefaultReaderFileGateway(ctx)),
                    DownloadFb2UseCase(DefaultFileDownloadGateway(ctx)),
                    InBookSearchMemory(),
                    ttsServiceLauncher = TtsServiceLauncher { },
                    player = FakeTtsPlaybackControls(),
                )
            vm.test(this) {
                expectInitialState()
                containerHost.load()
                skipItems(1) // loading = true
                assertTrue(awaitState().error != null)
            }
        }
}
