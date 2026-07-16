package ru.samlib.client.ui.illustrations

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import ru.samlib.client.domain.model.Image
import ru.samlib.client.domain.repository.IllustrationsRepository
import ru.samlib.client.domain.repository.SamlibResult
import ru.samlib.client.domain.repository.errorText

@Immutable
data class IllustrationsState(
    val loading: Boolean = false,
    val images: List<Image> = emptyList(),
    val error: String? = null,
)

class IllustrationsViewModel(
    private val link: String,
    private val repo: IllustrationsRepository,
) : ViewModel(),
    ContainerHost<IllustrationsState, Nothing> {
    override val container =
        viewModelScope.container<IllustrationsState, Nothing>(IllustrationsState())

    fun load() =
        intent {
            if (state.images.isNotEmpty()) return@intent
            reduce { state.copy(loading = true, error = null) }
            when (val result = repo.load(link)) {
                is SamlibResult.Success ->
                    reduce {
                        state.copy(loading = false, images = result.data, error = null)
                    }

                is SamlibResult.Error ->
                    reduce {
                        state.copy(loading = false, error = result.type.errorText())
                    }
            }
        }
}
