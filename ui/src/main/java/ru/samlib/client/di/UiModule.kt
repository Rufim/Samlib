package ru.samlib.client.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ru.samlib.client.domain.model.RatingSection
import ru.samlib.client.domain.repository.BlockTextExtractor
import ru.samlib.client.ui.auth.LoginViewModel
import ru.samlib.client.ui.author.AboutEditViewModel
import ru.samlib.client.ui.author.AuthorViewModel
import ru.samlib.client.ui.author.DirEditViewModel
import ru.samlib.client.ui.author.GroupEditViewModel
import ru.samlib.client.ui.author.WorkEditViewModel
import ru.samlib.client.ui.comments.CommentsViewModel
import ru.samlib.client.ui.downloads.DownloadsViewModel
import ru.samlib.client.ui.home.FollowedViewModel
import ru.samlib.client.ui.home.HistoryViewModel
import ru.samlib.client.ui.home.HomeViewModel
import ru.samlib.client.ui.illustrations.IllustrationsViewModel
import ru.samlib.client.ui.newest.NewestViewModel
import ru.samlib.client.ui.rating.RatingViewModel
import ru.samlib.client.ui.search.SearchViewModel
import ru.samlib.client.ui.work.ComposeBlockTextExtractor
import ru.samlib.client.ui.work.InBookSearchMemory
import ru.samlib.client.ui.work.LocalWorkViewModel
import ru.samlib.client.ui.work.SettingsViewModel
import ru.samlib.client.ui.work.WorkSearchViewModel
import ru.samlib.client.ui.work.WorkViewModel

val uiModule =
    module {
        viewModelOf(::NewestViewModel)
        viewModel { (link: String) -> AuthorViewModel(link, get(), get()) }
        viewModel { (link: String) -> AboutEditViewModel(link, get()) }
        viewModel { (link: String) -> DirEditViewModel(link, get()) }
        viewModel { (link: String) -> GroupEditViewModel(link, get()) }
        viewModel { (link: String, file: String?) -> WorkEditViewModel(link, file, get(), get(), get(), get()) }
        viewModel { (link: String, scrollTo: Int) ->
            WorkViewModel(
                link,
                scrollTo,
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }
        viewModel { (uri: String, scrollTo: Int) ->
            LocalWorkViewModel(
                uri,
                scrollTo,
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }
        viewModel { (link: String?, uri: String?, fromIndex: Int) ->
            WorkSearchViewModel(link, uri, fromIndex, get(), get())
        }
        viewModelOf(::HistoryViewModel)
        viewModelOf(::FollowedViewModel)
        viewModel { HomeViewModel(get(), get(), get(), get()) }
        viewModelOf(::SearchViewModel)
        viewModelOf(::SettingsViewModel)
        viewModel { (section: RatingSection) ->
            RatingViewModel(section, get())
        }
        viewModel { (link: String) -> CommentsViewModel(link, get(), get(), get()) }
        viewModel { LoginViewModel(get()) }
        viewModel { (link: String) -> IllustrationsViewModel(link, get()) }
        viewModel { DownloadsViewModel(get(), get()) }
        single<BlockTextExtractor> { ComposeBlockTextExtractor() }
        single { InBookSearchMemory() }
    }
