package ru.samlib.client.domain.repository

import ru.samlib.client.domain.model.Work
import ru.samlib.client.domain.model.WorkFilter

/** Запрос страницы выдачи: сколько пропустить и сколько взять. */
data class PageRequest(
    val skip: Int, // сколько элементов пропустить
    val size: Int, // размер страницы
)

interface NewestRepository {
    suspend fun getNewest(
        page: PageRequest,
        filter: WorkFilter,
        refresh: Boolean = false,
    ): SamlibResult<List<Work>>
}
