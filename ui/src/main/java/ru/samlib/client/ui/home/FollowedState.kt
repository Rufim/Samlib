package ru.samlib.client.ui.home

import androidx.compose.runtime.Immutable
import ru.samlib.client.domain.model.Author
import ru.samlib.client.domain.model.ObservedWork

@Immutable
data class FollowedState(
    val items: List<Author> = emptyList(),
    val books: List<UiObservedWork> = emptyList(),
)

sealed interface FollowedSideEffect {
    /** Автор удалён свайпом: снимок для восстановления по «Отменить». */
    data class AuthorDeleted(
        val author: Author,
        val works: List<ObservedWork>,
    ) : FollowedSideEffect

    /** Книга удалена свайпом. */
    data class BookDeleted(
        val work: ObservedWork,
    ) : FollowedSideEffect
}
