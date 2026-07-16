package ru.samlib.client.data.repository

import ru.samlib.client.data.parser.IllustrationsParser
import ru.samlib.client.data.remote.SamlibHttpClient
import ru.samlib.client.data.remote.samlibCatching
import ru.samlib.client.domain.model.Image
import ru.samlib.client.domain.repository.IllustrationsRepository
import ru.samlib.client.domain.repository.SamlibResult

class DefaultIllustrationsRepository(
    private val http: SamlibHttpClient,
    private val parser: IllustrationsParser,
) : IllustrationsRepository {
    override suspend fun load(link: String): SamlibResult<List<Image>> =
        samlibCatching { parser.parse(http.getPage(link)) }
}
