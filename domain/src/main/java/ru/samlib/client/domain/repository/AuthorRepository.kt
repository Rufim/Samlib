package ru.samlib.client.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.samlib.client.domain.model.Author
import ru.samlib.client.domain.model.ObservedWork

interface AuthorRepository {
    /** Только аватарка со страницы автора (без подгрузки разделов) — для шапки шторки. */
    suspend fun avatarOf(link: String): String?

    suspend fun getAuthor(link: String): SamlibResult<Author>

    fun observeFollowed(): Flow<List<Author>>

    /** Подписка на автора + baseline его книг (без пометок — чтобы не было ложных «new»). */
    suspend fun follow(author: Author)

    suspend fun unfollow(link: String)

    /**
     * Проверяет отслеживаемых авторов и standalone-книги на изменения.
     * Возвращает впервые изменившиеся книги — для уведомлений.
     */
    suspend fun checkFollowedUpdates(): List<ObservedWork>

    fun observeStandaloneWorks(): Flow<List<ObservedWork>>

    fun observeAuthorUpdates(authorLink: String): Flow<List<ObservedWork>>

    /** «Просмотрено» по автору: снимает пометки со всех его книг и бейдж. */
    suspend fun markSeen(authorLink: String)

    /** «Просмотрено» по одной книге (standalone в списке отслеживаемых). */
    suspend fun markWorkSeen(link: String)

    /** Открытие произведения: снять пометку; если у автора не осталось — снять бейдж. */
    suspend fun markWorkOpened(link: String)

    suspend fun deleteObservedWork(link: String)

    suspend fun restoreObservedWork(w: ObservedWork)

    /** Подписанный автор из БД (для снимка перед удалением), null — не подписан. */
    suspend fun followedAuthor(link: String): Author?

    suspend fun observedWorksOf(link: String): List<ObservedWork>

    /** Восстановление после свайп-удаления (отмена в снек-баре). */
    suspend fun restoreAuthor(
        author: Author,
        works: List<ObservedWork>,
    )

    suspend fun clearNotNotified(links: List<String>)

    /** Отслеживается ли книга: есть запись и не исключена, либо отслеживается её автор. */
    fun observeWorkObserved(
        link: String,
        authorLink: String,
    ): Flow<Boolean>

    /** Чекбокс «Отслеживать книгу» в меню читалки. */
    suspend fun toggleObserveWork(
        link: String,
        authorLink: String,
        title: String?,
    )
}
