package ru.samlib.client.ui.comments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import ru.samlib.client.domain.model.Comment
import ru.samlib.client.domain.repository.AuthRepository
import ru.samlib.client.domain.repository.CommentsRepository
import ru.samlib.client.domain.repository.SamlibResult
import ru.samlib.client.domain.repository.errorText
import ru.samlib.client.domain.usecase.PostCommentUseCase

class CommentsViewModel(
    private val link: String,
    private val repo: CommentsRepository,
    private val auth: AuthRepository,
    private val postComment: PostCommentUseCase,
) : ViewModel(),
    ContainerHost<CommentsState, CommentsSideEffect> {
    override val container =
        viewModelScope.container<CommentsState, CommentsSideEffect>(CommentsState())

    fun observeAuth() =
        intent {
            auth.isLoggedIn.collect { logged ->
                reduce { state.copy(loggedIn = logged) }
                // При входе первично заполняем поля автора из данных аккаунта.
                if (logged) {
                    val acc = auth.currentAccount()
                    reduce {
                        state.copy(
                            authorName = state.authorName.ifBlank { acc.name },
                            authorEmail = state.authorEmail.ifBlank { acc.email },
                            authorUrl = state.authorUrl.ifBlank { acc.url },
                        )
                    }
                }
            }
        }

    fun setAuthorName(v: String) = intent { reduce { state.copy(authorName = v) } }

    fun setAuthorEmail(v: String) = intent { reduce { state.copy(authorEmail = v) } }

    fun setAuthorUrl(v: String) = intent { reduce { state.copy(authorUrl = v) } }

    fun load() =
        intent {
            if (state.comments.isNotEmpty()) return@intent
            reduce { state.copy(loading = true, error = null) }
            when (val result = repo.load(link, 0, auth.currentCookie())) {
                is SamlibResult.Success ->
                    reduce {
                        state.copy(
                            loading = false,
                            comments = result.data.comments,
                            page = 0,
                            canLoadMore = result.data.hasMore,
                            error = null,
                        )
                    }

                is SamlibResult.Error ->
                    reduce {
                        state.copy(loading = false, error = result.type.errorText())
                    }
            }
        }

    fun loadMore() =
        intent {
            if (state.loadingMore || !state.canLoadMore || state.loading) return@intent
            reduce { state.copy(loadingMore = true) }
            val next = state.page + 1
            when (val result = repo.load(link, next, auth.currentCookie())) {
                is SamlibResult.Success ->
                    reduce {
                        state.copy(
                            loadingMore = false,
                            comments = state.comments + result.data.comments,
                            page = next,
                            canLoadMore = result.data.hasMore,
                        )
                    }

                is SamlibResult.Error -> reduce { state.copy(loadingMore = false) }
            }
        }

    fun refresh() =
        intent {
            reduce { state.copy(loading = true, error = null) }
            when (val result = repo.load(link, 0, auth.currentCookie())) {
                is SamlibResult.Success ->
                    reduce {
                        state.copy(
                            loading = false,
                            comments = result.data.comments,
                            page = 0,
                            canLoadMore = result.data.hasMore,
                            error = null,
                        )
                    }

                is SamlibResult.Error ->
                    reduce {
                        state.copy(loading = false, error = result.type.errorText())
                    }
            }
        }

    fun setReply(comment: Comment?) = intent { reduce { state.copy(replyTo = comment) } }

    fun send(text: String) =
        intent {
            if (text.isBlank() || state.sending) return@intent
            reduce { state.copy(sending = true) }
            val result =
                postComment(
                    PostCommentUseCase.Params(
                        commentsLink = link,
                        text = text,
                        authorName = state.authorName,
                        authorEmail = state.authorEmail,
                        authorUrl = state.authorUrl,
                        replyToMsgId = state.replyTo?.msgid,
                        loginCookie = auth.currentCookie(),
                    ),
                )
            reduce { state.copy(sending = false) }
            when (result) {
                PostCommentUseCase.Result.Posted -> {
                    postSideEffect(CommentsSideEffect.Posted)
                    // перезагрузка списка, чтобы показать новый комментарий
                    reduce { state.copy(comments = emptyList(), replyTo = null) }
                    load()
                }

                is PostCommentUseCase.Result.Rejected ->
                    postSideEffect(CommentsSideEffect.PostError(result.message))
            }
        }

    fun delete(comment: Comment) =
        intent {
            val msgid = comment.msgid ?: return@intent
            val cookie = auth.currentCookie()
            when (val result = repo.delete(link, msgid, cookie)) {
                is SamlibResult.Success -> {
                    postSideEffect(CommentsSideEffect.Posted)
                    reduce { state.copy(comments = emptyList()) }
                    load()
                }

                is SamlibResult.Error ->
                    postSideEffect(
                        CommentsSideEffect.PostError(
                            result.cause?.message ?: result.type.errorText(),
                        ),
                    )
            }
        }
}
