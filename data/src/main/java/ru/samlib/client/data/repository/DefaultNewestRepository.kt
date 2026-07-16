package ru.samlib.client.data.repository

import ru.samlib.client.data.parser.NewestParser
import ru.samlib.client.data.remote.SamlibHttpClient
import ru.samlib.client.data.remote.samlibCatching
import ru.samlib.client.domain.model.Work
import ru.samlib.client.domain.model.WorkFilter
import ru.samlib.client.domain.model.applyFilter
import ru.samlib.client.domain.repository.NewestRepository
import ru.samlib.client.domain.repository.PageRequest
import ru.samlib.client.domain.repository.SamlibResult

class DefaultNewestRepository(
    private val http: SamlibHttpClient,
    private val parser: NewestParser,
) : NewestRepository {
    private var cache: List<Work>? = null

    override suspend fun getNewest(
        page: PageRequest,
        filter: WorkFilter,
        refresh: Boolean,
    ): SamlibResult<List<Work>> =
        samlibCatching {
            val current = if (refresh) null else cache
            val all = current ?: parser.parse(http.getPage("/long.shtml")).also { cache = it }
            all.applyFilter(filter).drop(page.skip).take(page.size)
        }
}
