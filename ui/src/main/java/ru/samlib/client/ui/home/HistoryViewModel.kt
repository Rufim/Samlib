package ru.samlib.client.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import ru.samlib.client.domain.model.Work
import ru.samlib.client.domain.repository.WorkRepository
import ru.samlib.client.domain.usecase.FilterHistoryUseCase

class HistoryViewModel(
    private val workRepo: WorkRepository,
    private val filterHistory: FilterHistoryUseCase = FilterHistoryUseCase(),
) : ViewModel(),
    ContainerHost<HistoryState, HistorySideEffect> {
    override val container =
        viewModelScope.container<HistoryState, HistorySideEffect>(HistoryState())

    fun observe() =
        intent {
            workRepo.observeHistory().collect { works ->
                reduce {
                    state.copy(all = works, items = filterHistory(works, state.query, state.onlyPinned))
                }
            }
        }

    fun setQuery(q: String) =
        intent {
            reduce { state.copy(query = q, items = filterHistory(state.all, q, state.onlyPinned)) }
        }

    fun toggleOnlyPinned() =
        intent {
            val v = !state.onlyPinned
            reduce { state.copy(onlyPinned = v, items = filterHistory(state.all, state.query, v)) }
        }

    fun togglePin(link: String) =
        intent {
            val current = state.all.firstOrNull { it.link == link }?.pinned ?: false
            workRepo.setHistoryPinned(link, !current)
        }

    fun delete(link: String) =
        intent {
            val work = state.all.firstOrNull { it.link == link }
            workRepo.deleteHistory(link)
            if (work != null) postSideEffect(HistorySideEffect.Deleted(work))
        }

    fun restore(work: Work) = intent { workRepo.restoreHistory(work) }

    fun clear() = intent { workRepo.clearHistory() }
}
