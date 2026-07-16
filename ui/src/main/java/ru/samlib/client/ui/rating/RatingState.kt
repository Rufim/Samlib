package ru.samlib.client.ui.rating

import androidx.compose.runtime.Immutable
import ru.samlib.client.domain.model.RatingRow

@Immutable
data class RatingState(
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val rows: List<RatingRow> = emptyList(),
    val page: Int = 0,
    val canLoadMore: Boolean = false,
    val error: String? = null,
)

sealed interface RatingSideEffect {
    data class NavigateToAuthor(
        val link: String,
    ) : RatingSideEffect

    data class NavigateToWork(
        val link: String,
    ) : RatingSideEffect

    data class NavigateToComments(
        val link: String,
    ) : RatingSideEffect
}
