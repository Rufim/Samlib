package ru.samlib.client.domain.usecase

import ru.samlib.client.domain.model.Work

/** Фильтрация истории чтения по тексту запроса и признаку «только закреплённые». */
class FilterHistoryUseCase {
    operator fun invoke(
        list: List<Work>,
        query: String,
        onlyPinned: Boolean,
    ): List<Work> {
        val q = query.trim().lowercase()
        return list.filter { w ->
            (!onlyPinned || w.pinned) &&
                (
                    q.isEmpty() ||
                        w.title
                            .orEmpty()
                            .lowercase()
                            .contains(q) ||
                        w.authorShortName
                            .orEmpty()
                            .lowercase()
                            .contains(q)
                )
        }
    }
}
