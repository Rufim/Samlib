package ru.samlib.client.ui.downloads

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import ru.samlib.client.domain.model.SavedItem
import ru.samlib.client.domain.usecase.DeleteSavedItemUseCase
import ru.samlib.client.domain.usecase.LoadSavedItemsUseCase

@Immutable
data class DownloadsState(
    val loading: Boolean = false,
    val files: List<SavedItem> = emptyList(),
)

class DownloadsViewModel(
    private val loadSavedItems: LoadSavedItemsUseCase,
    private val deleteSavedItem: DeleteSavedItemUseCase,
) : ViewModel(),
    ContainerHost<DownloadsState, Nothing> {
    override val container = viewModelScope.container<DownloadsState, Nothing>(DownloadsState())

    fun load() =
        intent {
            reduce { state.copy(loading = true) }
            val files = loadSavedItems()
            reduce { state.copy(loading = false, files = files) }
        }

    fun delete(uri: String) =
        intent {
            deleteSavedItem(uri)
            reduce { state.copy(files = state.files.filterNot { it.uri == uri }) }
        }
}
