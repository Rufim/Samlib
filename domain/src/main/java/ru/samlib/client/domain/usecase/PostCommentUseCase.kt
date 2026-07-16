package ru.samlib.client.domain.usecase

import ru.samlib.client.domain.repository.CommentDraft
import ru.samlib.client.domain.repository.CommentsRepository
import ru.samlib.client.domain.repository.SamlibResult
import ru.samlib.client.domain.repository.errorText

/**
 * Отправка комментария: валидирует текст и имя автора (обязательно — либо задано вручную,
 * либо подставлено из аккаунта после входа), затем отправляет черновик через [CommentsRepository].
 */
class PostCommentUseCase(
    private val repo: CommentsRepository,
) {
    sealed interface Result {
        data object Posted : Result

        /** Как ошибка валидации (текст/имя пустые), так и ошибка сервера — сообщение для UI. */
        data class Rejected(
            val message: String,
        ) : Result
    }

    data class Params(
        val commentsLink: String,
        val text: String,
        val authorName: String,
        val authorEmail: String,
        val authorUrl: String,
        val replyToMsgId: String?,
        val loginCookie: String,
    )

    suspend operator fun invoke(params: Params): Result {
        val validationError =
            when {
                params.text.isBlank() -> "Комментарий не может быть пустым"
                params.authorName.isBlank() -> "Укажите имя или войдите на сайт"
                else -> null
            }
        if (validationError != null) return Result.Rejected(validationError)
        val draft =
            CommentDraft(
                text = params.text.trim(),
                name = params.authorName.trim(),
                email = params.authorEmail.trim(),
                url = params.authorUrl.trim(),
                msgid = params.replyToMsgId,
            )
        return when (val result = repo.post(params.commentsLink, draft, params.loginCookie)) {
            is SamlibResult.Success -> Result.Posted
            is SamlibResult.Error -> Result.Rejected(result.cause?.message ?: result.type.errorText())
        }
    }
}
