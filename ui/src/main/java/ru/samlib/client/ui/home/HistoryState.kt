package ru.samlib.client.ui.home

import androidx.compose.runtime.Immutable
import ru.samlib.client.domain.model.Work

@Immutable
data class HistoryState(
    val all: List<Work> = emptyList(),
    val items: List<Work> = emptyList(),
    val query: String = "",
    val onlyPinned: Boolean = false,
)

sealed interface HistorySideEffect {
    data class Deleted(
        val work: Work,
    ) : HistorySideEffect
}
