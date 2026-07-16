package ru.samlib.client.ui.author

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import ru.samlib.client.domain.repository.AuthorEditRepository
import ru.samlib.client.domain.repository.DirFields
import ru.samlib.client.domain.repository.DirForm
import ru.samlib.client.domain.repository.SamlibResult
import ru.samlib.client.domain.repository.errorText
import ru.samlib.client.ui.common.EditSideEffect

@Immutable
data class DirEditState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val loaded: Boolean = false,
    val author: String = "",
    val title: String = "",
    val email: String = "",
    val homeLink: String = "",
    val annotation: String = "",
    val error: String? = null,
)

class DirEditViewModel(
    private val authorLink: String,
    private val repo: AuthorEditRepository,
) : ViewModel(),
    ContainerHost<DirEditState, EditSideEffect> {
    override val container = viewModelScope.container<DirEditState, EditSideEffect>(DirEditState())

    private var form: DirForm? = null

    fun load() =
        intent {
            reduce { state.copy(loading = true, error = null) }
            when (val result = repo.loadDir(authorLink)) {
                is SamlibResult.Success -> {
                    form = result.data
                    reduce {
                        state.copy(
                            loading = false,
                            loaded = true,
                            author = result.data.author,
                            title = result.data.title,
                            email = result.data.email,
                            homeLink = result.data.homeLink,
                            annotation = result.data.annotation,
                        )
                    }
                }

                is SamlibResult.Error ->
                    reduce {
                        state.copy(
                            loading = false,
                            error = result.type.errorText(),
                        )
                    }
            }
        }

    fun setAuthor(v: String) = intent { reduce { state.copy(author = v) } }

    fun setTitle(v: String) = intent { reduce { state.copy(title = v) } }

    fun setEmail(v: String) = intent { reduce { state.copy(email = v) } }

    fun setHomeLink(v: String) = intent { reduce { state.copy(homeLink = v) } }

    fun setAnnotation(v: String) = intent { reduce { state.copy(annotation = v) } }

    fun save() =
        intent {
            val current = form ?: return@intent
            if (state.saving) return@intent
            reduce { state.copy(saving = true) }
            val result =
                repo.saveDir(
                    current,
                    fields =
                        DirFields(
                            author = state.author.trim(),
                            title = state.title.trim(),
                            email = state.email.trim(),
                            homeLink = state.homeLink.trim(),
                            annotation = state.annotation.trim(),
                        ),
                )
            reduce { state.copy(saving = false) }
            when (result) {
                is SamlibResult.Success -> postSideEffect(EditSideEffect.Saved)
                is SamlibResult.Error ->
                    postSideEffect(
                        EditSideEffect.Error(
                            result.cause?.message ?: result.type.errorText(),
                        ),
                    )
            }
        }
}
