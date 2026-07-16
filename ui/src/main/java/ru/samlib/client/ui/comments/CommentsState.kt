package ru.samlib.client.ui.comments

import androidx.compose.runtime.Immutable
import ru.samlib.client.domain.model.Comment

@Immutable
data class CommentsState(
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val comments: List<Comment> = emptyList(),
    val page: Int = 0,
    val canLoadMore: Boolean = false,
    val error: String? = null,
    val loggedIn: Boolean = false,
    val sending: Boolean = false,
    val replyTo: Comment? = null,
    // Данные автора для формы (аноним заполняет вручную; при входе — из аккаунта).
    val authorName: String = "",
    val authorEmail: String = "",
    val authorUrl: String = "",
)

sealed interface CommentsSideEffect {
    data object Posted : CommentsSideEffect

    data class PostError(
        val message: String,
    ) : CommentsSideEffect
}
