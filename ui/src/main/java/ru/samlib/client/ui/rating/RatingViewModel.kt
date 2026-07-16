package ru.samlib.client.ui.rating

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import ru.samlib.client.domain.model.RatingSection
import ru.samlib.client.domain.repository.RatingRepository
import ru.samlib.client.domain.repository.SamlibResult
import ru.samlib.client.domain.repository.errorText

class RatingViewModel(
    private val section: RatingSection,
    private val repo: RatingRepository,
) : ViewModel(),
    ContainerHost<RatingState, RatingSideEffect> {
    override val container = viewModelScope.container<RatingState, RatingSideEffect>(RatingState())

    fun load() =
        intent {
            if (state.rows.isNotEmpty()) return@intent
            reduce { state.copy(loading = true, error = null) }
            when (val result = repo.load(section, 0)) {
                is SamlibResult.Success ->
                    reduce {
                        state.copy(
                            loading = false,
                            rows = result.data.rows,
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
            when (val result = repo.load(section, next)) {
                is SamlibResult.Success ->
                    reduce {
                        state.copy(
                            loadingMore = false,
                            rows = state.rows + result.data.rows,
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
            when (val result = repo.load(section, 0)) {
                is SamlibResult.Success ->
                    reduce {
                        state.copy(
                            loading = false,
                            rows = result.data.rows,
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

    fun onAuthorClick(link: String) =
        intent {
            postSideEffect(RatingSideEffect.NavigateToAuthor(link))
        }

    fun onWorkClick(link: String) =
        intent {
            postSideEffect(RatingSideEffect.NavigateToWork(link))
        }

    fun onCommentsClick(link: String) =
        intent {
            postSideEffect(RatingSideEffect.NavigateToComments(link))
        }
}
