package ru.samlib.client.di

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.binds
import org.koin.dsl.module
import ru.samlib.client.data.db.ALL_MIGRATIONS
import ru.samlib.client.data.db.SamlibDatabase
import ru.samlib.client.data.local.DefaultDownloadsRepository
import ru.samlib.client.data.local.DefaultLegacyImporter
import ru.samlib.client.data.local.DefaultLocalFileRepository
import ru.samlib.client.data.local.DefaultReaderFileGateway
import ru.samlib.client.data.local.DefaultSavedBooksRepository
import ru.samlib.client.data.network.DefaultOfflineStatus
import ru.samlib.client.data.network.NetworkConfigurator
import ru.samlib.client.data.parser.AuthorEditParser
import ru.samlib.client.data.parser.AuthorParser
import ru.samlib.client.data.parser.CommentsParser
import ru.samlib.client.data.parser.DefaultWorkContentParser
import ru.samlib.client.data.parser.DefaultWorkDraftChunker
import ru.samlib.client.data.parser.DiscussionParser
import ru.samlib.client.data.parser.IllustrationsParser
import ru.samlib.client.data.parser.NewestParser
import ru.samlib.client.data.parser.SearchParser
import ru.samlib.client.data.parser.TopAuthorsParser
import ru.samlib.client.data.parser.WebSearchParser
import ru.samlib.client.data.parser.WorkParser
import ru.samlib.client.data.parser.WorkRatingParser
import ru.samlib.client.data.remote.DefaultFileDownloadGateway
import ru.samlib.client.data.remote.SamlibHttpClient
import ru.samlib.client.data.repository.DefaultAuthRepository
import ru.samlib.client.data.repository.DefaultAuthorEditRepository
import ru.samlib.client.data.repository.DefaultAuthorRepository
import ru.samlib.client.data.repository.DefaultCommentsRepository
import ru.samlib.client.data.repository.DefaultIllustrationsRepository
import ru.samlib.client.data.repository.DefaultNewestRepository
import ru.samlib.client.data.repository.DefaultRatingRepository
import ru.samlib.client.data.repository.DefaultSearchRepository
import ru.samlib.client.data.repository.DefaultWorkRepository
import ru.samlib.client.data.settings.DefaultReaderSettingsRepository
import ru.samlib.client.data.translate.DefaultTranslator
import ru.samlib.client.data.tts.TtsPlayer
import ru.samlib.client.domain.repository.AuthRepository
import ru.samlib.client.domain.repository.AuthorEditRepository
import ru.samlib.client.domain.repository.AuthorRepository
import ru.samlib.client.domain.repository.CommentsRepository
import ru.samlib.client.domain.repository.DownloadsRepository
import ru.samlib.client.domain.repository.FileDownloadGateway
import ru.samlib.client.domain.repository.IllustrationsRepository
import ru.samlib.client.domain.repository.LegacyImporter
import ru.samlib.client.domain.repository.LocalFileRepository
import ru.samlib.client.domain.repository.NewestRepository
import ru.samlib.client.domain.repository.OfflineStatus
import ru.samlib.client.domain.repository.RatingRepository
import ru.samlib.client.domain.repository.ReaderFileGateway
import ru.samlib.client.domain.repository.ReaderSettingsRepository
import ru.samlib.client.domain.repository.SavedBooksRepository
import ru.samlib.client.domain.repository.SearchRepository
import ru.samlib.client.domain.repository.Translator
import ru.samlib.client.domain.repository.TtsEngineSettings
import ru.samlib.client.domain.repository.TtsPlaybackControls
import ru.samlib.client.domain.repository.WorkContentParser
import ru.samlib.client.domain.repository.WorkDraftChunker
import ru.samlib.client.domain.repository.WorkRepository
import ru.samlib.client.domain.usecase.DeleteSavedItemUseCase
import ru.samlib.client.domain.usecase.DownloadFb2UseCase
import ru.samlib.client.domain.usecase.FilterHistoryUseCase
import ru.samlib.client.domain.usecase.ImportFollowedAuthorsUseCase
import ru.samlib.client.domain.usecase.ImportLegacyDataUseCase
import ru.samlib.client.domain.usecase.LoadBookTextUseCase
import ru.samlib.client.domain.usecase.LoadSavedItemsUseCase
import ru.samlib.client.domain.usecase.LoadWorkEditUseCase
import ru.samlib.client.domain.usecase.LoadWorkUseCase
import ru.samlib.client.domain.usecase.OpenExternallyUseCase
import ru.samlib.client.domain.usecase.PickLocalFileUseCase
import ru.samlib.client.domain.usecase.PostCommentUseCase
import ru.samlib.client.domain.usecase.SaveWorkEditUseCase
import ru.samlib.client.domain.usecase.SaveWorkUseCase
import ru.samlib.client.domain.usecase.UnfollowAuthorUseCase

val networkModule =
    module {
        single { SamlibHttpClient() }
        single<OfflineStatus> { DefaultOfflineStatus(get(), get()) }
        // eagerly: применяет домен/прокси из настроек к HTTP-клиенту при старте
        single(createdAtStart = true) { NetworkConfigurator(get(), get()) }
    }

val dbModule =
    module {
        single {
            Room
                .databaseBuilder(androidContext(), SamlibDatabase::class.java, "samlib.db")
                .addMigrations(*ALL_MIGRATIONS)
                .fallbackToDestructiveMigration()
                .build()
        }
        single { get<SamlibDatabase>().authorDao() }
        single { get<SamlibDatabase>().bookmarkDao() }
        single { get<SamlibDatabase>().historyDao() }
        single { get<SamlibDatabase>().cachedWorkDao() }
        single { get<SamlibDatabase>().savedBookDao() }
        single { get<SamlibDatabase>().observedWorkDao() }
    }

val parserModule =
    module {
        single { NewestParser() }
        single { AuthorParser() }
        single { WorkParser() }
        single<WorkContentParser> { DefaultWorkContentParser() }
        single<WorkDraftChunker> { DefaultWorkDraftChunker(get()) }
        single { SearchParser() }
        single { WebSearchParser() }
        single { TopAuthorsParser() }
        single { WorkRatingParser() }
        single { DiscussionParser() }
        single { CommentsParser() }
        single { IllustrationsParser() }
        single { AuthorEditParser() }
    }

val repositoryModule =
    module {
        // datastore настроек читалки
        single {
            PreferenceDataStoreFactory.create(
                produceFile = { androidContext().preferencesDataStoreFile("reader_settings") },
            )
        }
        single<ReaderSettingsRepository> { DefaultReaderSettingsRepository(get()) }
        single<LocalFileRepository> { DefaultLocalFileRepository(androidContext(), get()) }

        single<NewestRepository> { DefaultNewestRepository(get(), get()) }
        single<AuthorRepository> { DefaultAuthorRepository(get(), get(), get(), get(), get()) }
        single<WorkRepository> {
            DefaultWorkRepository(
                androidContext(),
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }
        single<SearchRepository> { DefaultSearchRepository(get(), get(), get()) }
        single<RatingRepository> { DefaultRatingRepository(get(), get(), get(), get()) }
        single<CommentsRepository> { DefaultCommentsRepository(get(), get()) }
        single<IllustrationsRepository> { DefaultIllustrationsRepository(get(), get()) }
        single<DownloadsRepository> { DefaultDownloadsRepository(androidContext()) }
        single<SavedBooksRepository> { DefaultSavedBooksRepository(androidContext(), get()) }
        single<LegacyImporter> { DefaultLegacyImporter(androidContext(), get(), get(), get(), get()) }
        single<AuthRepository> { DefaultAuthRepository(get(), get()) }
        single<AuthorEditRepository> { DefaultAuthorEditRepository(get(), get(), get()) }

        single<Translator> { DefaultTranslator() }
        single<ReaderFileGateway> { DefaultReaderFileGateway(androidContext()) }
        single<FileDownloadGateway> { DefaultFileDownloadGateway(androidContext()) }
        factory { LoadWorkUseCase(get(), get(), get()) }
        factory { LoadBookTextUseCase(get(), get(), get(), get()) }
        factory { SaveWorkUseCase(get()) }
        factory { OpenExternallyUseCase(get()) }
        factory { DownloadFb2UseCase(get()) }
        factory { PickLocalFileUseCase(get()) }
        factory { FilterHistoryUseCase() }
        factory { ImportLegacyDataUseCase(get(), get()) }
        factory { LoadSavedItemsUseCase(get(), get()) }
        factory { DeleteSavedItemUseCase(get(), get()) }
        factory { UnfollowAuthorUseCase(get()) }
        factory { ImportFollowedAuthorsUseCase(get()) }
        factory { PostCommentUseCase(get()) }
        factory { LoadWorkEditUseCase(get(), get()) }
        factory { SaveWorkEditUseCase(get()) }

        single {
            TtsPlayer(
                androidContext(),
                get(),
                saveProgress = { link, block -> get<WorkRepository>().saveReadingPosition(link, block) },
                flushProgress = { link -> get<WorkRepository>().flushReadingPosition(link) },
            )
        } binds arrayOf(TtsEngineSettings::class, TtsPlaybackControls::class)
    }

/** Совокупность data-модулей — регистрируется в App одним списком. */
val dataModules = listOf(networkModule, dbModule, parserModule, repositoryModule)
