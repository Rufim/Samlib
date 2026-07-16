package ru.samlib.client.ui.newest

import androidx.compose.runtime.Immutable
import ru.samlib.client.domain.model.Work
import ru.samlib.client.domain.model.WorkFilter

@Immutable
data class NewestState(
    val filter: WorkFilter = WorkFilter(),
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val items: List<Work> = emptyList(),
    val page: Int = 0,
    val canLoadMore: Boolean = false,
    val error: String? = null,
)

sealed interface NewestSideEffect {
    data class NavigateToAuthor(
        val link: String,
    ) : NewestSideEffect
}
