package ru.samlib.client.domain.usecase

import ru.samlib.client.domain.model.Author
import ru.samlib.client.domain.model.ObservedWork
import ru.samlib.client.domain.repository.AuthorRepository

/**
 * Отписка от автора со снимком «до» — чтобы экран мог предложить «Отменить» (снек-бар) и
 * восстановить автора вместе с его книгами под наблюдением, если пользователь передумает.
 */
class UnfollowAuthorUseCase(
    private val authorRepo: AuthorRepository,
) {
    data class Snapshot(
        val author: Author,
        val works: List<ObservedWork>,
    )

    /** Снимок автора+книг для «Отменить», либо null, если автор не найден среди отслеживаемых. */
    suspend operator fun invoke(link: String): Snapshot? {
        val author = authorRepo.followedAuthor(link) ?: return null
        val works = authorRepo.observedWorksOf(link)
        authorRepo.unfollow(link)
        return Snapshot(author, works)
    }
}
