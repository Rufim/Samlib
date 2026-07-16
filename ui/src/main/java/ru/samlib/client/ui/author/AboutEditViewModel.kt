package ru.samlib.client.ui.author

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import ru.samlib.client.domain.repository.AboutForm
import ru.samlib.client.domain.repository.AuthorEditRepository
import ru.samlib.client.domain.repository.ImageUpload
import ru.samlib.client.domain.repository.SamlibResult
import ru.samlib.client.domain.repository.errorText

@Immutable
data class AboutEditState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val text: String = "",
    val loaded: Boolean = false,
    val error: String? = null,
)

sealed interface AboutEditSideEffect {
    data object Saved : AboutEditSideEffect

    data class Error(
        val message: String,
    ) : AboutEditSideEffect

    /** Результат загрузки аватара (тост, без навигации). */
    data class AvatarResult(
        val message: String,
    ) : AboutEditSideEffect
}

class AboutEditViewModel(
    private val authorLink: String,
    private val repo: AuthorEditRepository,
) : ViewModel(),
    ContainerHost<AboutEditState, AboutEditSideEffect> {
    override val container =
        viewModelScope.container<AboutEditState, AboutEditSideEffect>(AboutEditState())

    private var form: AboutForm? = null

    fun load() =
        intent {
            reduce { state.copy(loading = true, error = null) }
            when (val result = repo.loadAbout(authorLink)) {
                is SamlibResult.Success -> {
                    form = result.data
                    reduce { state.copy(loading = false, loaded = true, text = result.data.body) }
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

    fun setText(value: String) = intent { reduce { state.copy(text = value) } }

    fun uploadAvatar(
        bytes: ByteArray,
        mime: String,
    ) = intent {
        if (state.saving) return@intent
        reduce { state.copy(saving = true) }
        val result = repo.uploadAvatar(authorLink, ImageUpload(bytes, mime))
        reduce { state.copy(saving = false) }
        val message =
            when (result) {
                is SamlibResult.Success -> "Аватар обновлён"
                is SamlibResult.Error -> result.cause?.message ?: "Не удалось загрузить аватар"
            }
        postSideEffect(AboutEditSideEffect.AvatarResult(message))
    }

    fun save() =
        intent {
            val current = form ?: return@intent
            if (state.saving) return@intent
            reduce { state.copy(saving = true) }
            val result = repo.saveAbout(current, state.text.trim())
            reduce { state.copy(saving = false) }
            when (result) {
                is SamlibResult.Success -> postSideEffect(AboutEditSideEffect.Saved)
                is SamlibResult.Error ->
                    postSideEffect(
                        AboutEditSideEffect.Error(
                            result.cause?.message ?: result.type.errorText(),
                        ),
                    )
            }
        }
}
