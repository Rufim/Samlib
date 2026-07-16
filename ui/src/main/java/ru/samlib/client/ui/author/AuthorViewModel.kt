package ru.samlib.client.ui.author

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import ru.samlib.client.domain.model.Author
import ru.samlib.client.domain.repository.AuthRepository
import ru.samlib.client.domain.repository.AuthorRepository
import ru.samlib.client.domain.repository.SamlibResult
import ru.samlib.client.domain.repository.errorText
import ru.samlib.client.domain.samlib.SamlibLinkFormat
import ru.samlib.client.domain.samlib.SamlibUrls

class AuthorViewModel(
    private val link: String,
    private val repo: AuthorRepository,
    private val authRepo: AuthRepository,
) : ViewModel(),
    ContainerHost<AuthorState, AuthorSideEffect> {
    override val container =
        viewModelScope.container<AuthorState, AuthorSideEffect>(AuthorState())

    /** Определяет, открыта ли собственная страница автора (по URL аккаунта). */
    fun checkOwnPage() =
        intent {
            val accPath =
                authRepo
                    .currentAccount()
                    .url
                    .substringAfter(SamlibUrls.HOST, "")
                    .trim('/')
            val own = accPath.isNotEmpty() && link.trim('/') == accPath
            reduce { state.copy(isOwnPage = own) }
        }

    fun load() =
        intent {
            reduce { state.copy(loading = true, error = null) }
            when (val result = repo.getAuthor(link)) {
                is SamlibResult.Success ->
                    reduce {
                        state.copy(loading = false, author = result.data, error = null)
                    }

                is SamlibResult.Error ->
                    reduce {
                        state.copy(loading = false, error = result.type.errorText())
                    }
            }
        }

    fun onWorkClick(link: String) =
        intent {
            postSideEffect(AuthorSideEffect.OpenWork(link))
        }

    /** Клик по счётчику комментариев книги → открыть её обсуждение. */
    fun onCommentsClick(workLink: String) =
        intent {
            val commentsLink =
                SamlibLinkFormat.COMMENT_PREFIX + workLink.removeSuffix(SamlibLinkFormat.WORK_SUFFIX)
            postSideEffect(AuthorSideEffect.OpenComments(commentsLink))
        }

    /** Клик по «редактировать» у своей книги → редактор с именем файла книги. */
    fun onEditWorkClick(workLink: String) =
        intent {
            val file = workLink.substringAfterLast('/').removeSuffix(SamlibLinkFormat.WORK_SUFFIX)
            postSideEffect(AuthorSideEffect.EditWork(link, file))
        }

    fun observeFollow() =
        intent {
            repo.observeFollowed().collect { list ->
                reduce { state.copy(isFollowed = list.any { it.link == link }) }
            }
        }

    /** Пометки «обновлено» из observed_works: книги и содержащие их разделы. */
    fun observeUpdates() =
        intent {
            repo.observeAuthorUpdates(link).collect { works ->
                val changed = works.filter { it.changed }
                reduce {
                    state.copy(
                        changedLinks = changed.map { it.link }.toSet(),
                        changedCategories = changed.mapNotNull { it.categoryTitle }.toSet(),
                    )
                }
            }
        }

    fun toggleSection(title: String) =
        intent {
            reduce {
                state.copy(
                    collapsed =
                        if (title in state.collapsed) {
                            state.collapsed - title
                        } else {
                            state.collapsed + title
                        },
                )
            }
        }

    fun toggleFollow() =
        intent {
            if (state.isFollowed) {
                repo.unfollow(link)
            } else {
                repo.follow(state.author ?: Author(link = link))
            }
        }
}
