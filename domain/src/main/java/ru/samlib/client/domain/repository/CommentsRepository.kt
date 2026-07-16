package ru.samlib.client.domain.repository

import ru.samlib.client.domain.model.CommentsPage

/** Черновик отправляемого комментария. [msgid] задан только для ответа на комментарий. */
data class CommentDraft(
    val text: String, // тело комментария
    val name: String, // отображаемое имя автора
    val email: String, // e-mail (может быть пустым)
    val url: String = "", // ссылка на сайт автора (необязательна)
    val msgid: String? = null, // MSGID родителя при ответе; null — новый комментарий
)

/**
 * Грузит страницу комментариев произведения. Пагинация через `?PAGE=n`
 * (стр. 0 → PAGE=1 = самые новые, дальше — старее). Количество страниц —
 * из маркера `Страниц (N)`.
 */
interface CommentsRepository {
    suspend fun load(
        link: String,
        page: Int,
        loginCookie: String = "",
    ): SamlibResult<CommentsPage>

    /**
     * Отправка нового комментария. [commentsLink] вида `/comment/…`; произведение —
     * тот же путь без префикса `/comment`. Требуется cookie авторизации.
     */
    suspend fun post(
        commentsLink: String,
        draft: CommentDraft,
        loginCookie: String,
    ): SamlibResult<Unit>

    /** Удаление своего комментария по MSGID. */
    suspend fun delete(
        commentsLink: String,
        msgid: String,
        loginCookie: String,
    ): SamlibResult<Unit>
}
