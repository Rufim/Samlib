package ru.samlib.client.ui.newest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import ru.samlib.client.domain.model.WorkFilter
import ru.samlib.client.domain.repository.NewestRepository
import ru.samlib.client.domain.repository.PageRequest
import ru.samlib.client.domain.repository.SamlibResult
import ru.samlib.client.domain.repository.errorText

class NewestViewModel(
    private val repo: NewestRepository,
) : ViewModel(),
    ContainerHost<NewestState, NewestSideEffect> {
    override val container =
        viewModelScope.container<NewestState, NewestSideEffect>(NewestState())

    fun load() =
        intent {
            reduce { state.copy(loading = true, error = null) }
            when (val result = repo.getNewest(PageRequest(0, PAGE_SIZE), state.filter)) {
                is SamlibResult.Success ->
                    reduce {
                        state.copy(
                            loading = false,
                            items = result.data,
                            page = 0,
                            canLoadMore = result.data.size == PAGE_SIZE,
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
            when (val result = repo.getNewest(PageRequest(next * PAGE_SIZE, PAGE_SIZE), state.filter)) {
                is SamlibResult.Success ->
                    reduce {
                        state.copy(
                            loadingMore = false,
                            items = state.items + result.data,
                            page = next,
                            canLoadMore = result.data.size == PAGE_SIZE,
                        )
                    }

                is SamlibResult.Error -> reduce { state.copy(loadingMore = false) }
            }
        }

    fun refresh() =
        intent {
            reduce { state.copy(loading = true, error = null) }
            when (
                val result =
                    repo.getNewest(PageRequest(0, PAGE_SIZE), state.filter, refresh = true)
            ) {
                is SamlibResult.Success ->
                    reduce {
                        state.copy(
                            loading = false,
                            items = result.data,
                            page = 0,
                            canLoadMore = result.data.size == PAGE_SIZE,
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
            postSideEffect(NewestSideEffect.NavigateToAuthor(link))
        }

    fun applyFilter(filter: WorkFilter) =
        intent {
            reduce { state.copy(loading = true, error = null, filter = filter) }
            when (val result = repo.getNewest(PageRequest(0, PAGE_SIZE), filter, refresh = false)) {
                is SamlibResult.Success ->
                    reduce {
                        state.copy(
                            loading = false,
                            items = result.data,
                            page = 0,
                            canLoadMore = result.data.size == PAGE_SIZE,
                            error = null,
                        )
                    }

                is SamlibResult.Error ->
                    reduce {
                        state.copy(loading = false, error = result.type.errorText())
                    }
            }
        }

    private companion object {
        const val PAGE_SIZE = 50
    }
}
