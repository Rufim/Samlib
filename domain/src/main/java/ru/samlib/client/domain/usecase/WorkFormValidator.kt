package ru.samlib.client.domain.usecase

import ru.samlib.client.domain.usecase.WorkFormValidator.MIN_BODY_BYTES

/** Причина отказа валидации формы произведения (текст сообщения выбирает UI). */
sealed interface WorkFormError {
    data object EmptyTitle : WorkFormError

    data object BadFileName : WorkFormError

    data object TextTooShort : WorkFormError
}

/**
 * Правила формы new_book/edit_book самиздата: имя файла — латиница/цифры/-/_,
 * тело — не короче [MIN_BODY_BYTES] байт cp1251 (сервер отвечает «не передали
 * сам текст» на более короткие).
 */
object WorkFormValidator {
    const val MIN_BODY_BYTES = 60
    private val FILE_NAME_RE = Regex("[a-z0-9_-]+")

    /** [fileName] == null — режим редактирования, имя не проверяется. */
    fun validate(
        fileName: String?,
        title: String,
        bodyBytes: Int,
    ): WorkFormError? =
        when {
            title.isBlank() -> WorkFormError.EmptyTitle
            fileName != null && !fileName.matches(FILE_NAME_RE) -> WorkFormError.BadFileName
            bodyBytes < MIN_BODY_BYTES -> WorkFormError.TextTooShort
            else -> null
        }
}
