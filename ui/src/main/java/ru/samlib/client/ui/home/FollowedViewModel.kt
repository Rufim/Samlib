package ru.samlib.client.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import ru.samlib.client.domain.model.Author
import ru.samlib.client.domain.model.ObservedWork
import ru.samlib.client.domain.repository.AuthorRepository
import ru.samlib.client.domain.usecase.ImportFollowedAuthorsUseCase
import ru.samlib.client.domain.usecase.UnfollowAuthorUseCase

class FollowedViewModel(
    private val authorRepo: AuthorRepository,
    private val unfollowAuthor: UnfollowAuthorUseCase,
    private val importFollowedAuthors: ImportFollowedAuthorsUseCase,
) : ViewModel(),
    ContainerHost<FollowedState, FollowedSideEffect> {
    override val container =
        viewModelScope.container<FollowedState, FollowedSideEffect>(FollowedState())

    fun observe() =
        intent {
            authorRepo.observeFollowed().collect { authors -> reduce { state.copy(items = authors) } }
        }

    fun observeBooks() =
        intent {
            authorRepo
                .observeStandaloneWorks()
                .collect { books -> reduce { state.copy(books = books.toUi()) } }
        }

    /** Удаление свайпом: снимок в side effect — для «Отменить» в снек-баре. */
    fun unfollow(link: String) =
        intent {
            val snapshot = unfollowAuthor(link) ?: return@intent
            postSideEffect(FollowedSideEffect.AuthorDeleted(snapshot.author, snapshot.works))
        }

    fun restoreAuthor(
        author: Author,
        works: List<ObservedWork>,
    ) = intent {
        authorRepo.restoreAuthor(author, works)
    }

    fun deleteBook(work: ObservedWork) =
        intent {
            authorRepo.deleteObservedWork(work.link)
            postSideEffect(FollowedSideEffect.BookDeleted(work))
        }

    fun restoreBook(work: ObservedWork) = intent { authorRepo.restoreObservedWork(work) }

    /** «Просмотрено»: снять пометки обновлений у автора / у книги. */
    fun markSeen(link: String) = intent { authorRepo.markSeen(link) }

    fun markBookSeen(link: String) = intent { authorRepo.markWorkSeen(link) }

    /** Текст для экспорта: строки «link\tИмя». */
    fun exportText(): String =
        container.stateFlow.value.items
            .joinToString("\n") { "${it.link}\t${it.fullName.orEmpty()}" }

    /** Импорт: строки «link[\tИмя]». */
    fun import(text: String) = intent { importFollowedAuthors(text) }
}
