package ru.samlib.client.ui.author

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import ru.samlib.client.domain.repository.AuthorEditRepository
import ru.samlib.client.domain.repository.GroupEntry
import ru.samlib.client.domain.repository.GroupForm
import ru.samlib.client.domain.repository.SamlibResult
import ru.samlib.client.domain.repository.errorText
import ru.samlib.client.ui.common.EditSideEffect

/** Редактируемая группа: название + аннотация. */
data class EditableGroup(
    val title: String = "",
    val annotation: String = "",
)

@Immutable
data class GroupEditState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val loaded: Boolean = false,
    val groups: List<EditableGroup> = emptyList(),
    val max: Int = 0,
    val error: String? = null,
) {
    val canAdd: Boolean get() = groups.size < max
}

class GroupEditViewModel(
    private val authorLink: String,
    private val repo: AuthorEditRepository,
) : ViewModel(),
    ContainerHost<GroupEditState, EditSideEffect> {
    override val container =
        viewModelScope.container<GroupEditState, EditSideEffect>(GroupEditState())

    private var form: GroupForm? = null

    fun load() =
        intent {
            reduce { state.copy(loading = true, error = null) }
            when (val result = repo.loadGroups(authorLink)) {
                is SamlibResult.Success -> {
                    form = result.data
                    reduce {
                        state.copy(
                            loading = false,
                            loaded = true,
                            max = result.data.max,
                            groups = result.data.groups.map { EditableGroup(it.title, it.annotation) },
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

    fun setTitle(
        index: Int,
        value: String,
    ) = updateAt(index) { it.copy(title = value) }

    fun setAnnotation(
        index: Int,
        value: String,
    ) = updateAt(index) { it.copy(annotation = value) }

    private fun updateAt(
        index: Int,
        transform: (EditableGroup) -> EditableGroup,
    ) = intent {
        reduce {
            state.copy(groups = state.groups.mapIndexed { i, g -> if (i == index) transform(g) else g })
        }
    }

    fun addGroup() =
        intent {
            if (state.groups.size >= state.max) return@intent
            reduce { state.copy(groups = state.groups + EditableGroup()) }
        }

    fun removeGroup(index: Int) =
        intent {
            reduce { state.copy(groups = state.groups.filterIndexed { i, _ -> i != index }) }
        }

    fun save() =
        intent {
            val current = form ?: return@intent
            if (state.saving) return@intent
            reduce { state.copy(saving = true) }
            val groups =
                state.groups
                    .map { GroupEntry(it.title.trim(), it.annotation.trim()) }
                    .filter { it.title.isNotBlank() || it.annotation.isNotBlank() }
            val result = repo.saveGroups(current, groups)
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
