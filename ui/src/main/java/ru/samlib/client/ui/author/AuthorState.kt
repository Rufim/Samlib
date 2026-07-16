package ru.samlib.client.ui.author

import androidx.compose.runtime.Immutable
import ru.samlib.client.domain.model.Author

@Immutable
data class AuthorState(
    val loading: Boolean = false,
    val author: Author? = null,
    val isFollowed: Boolean = false,
    val error: String? = null,
    val collapsed: Set<String> = emptySet(),
    /** Ссылки книг с непросмотренными обновлениями (для метки «обновлено»). */
    val changedLinks: Set<String> = emptySet(),
    /** Названия разделов, содержащих обновлённые книги. */
    val changedCategories: Set<String> = emptySet(),
    /** Своя ли это страница автора (доступно редактирование раздела/книг). */
    val isOwnPage: Boolean = false,
)

sealed interface AuthorSideEffect {
    data class OpenWork(
        val link: String,
    ) : AuthorSideEffect

    /** Открыть комментарии к книге по уже построенной ссылке. */
    data class OpenComments(
        val link: String,
    ) : AuthorSideEffect

    /** Редактировать своё произведение: ссылка автора + имя файла книги. */
    data class EditWork(
        val link: String,
        val file: String,
    ) : AuthorSideEffect
}
