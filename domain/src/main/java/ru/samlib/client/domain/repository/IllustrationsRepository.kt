package ru.samlib.client.domain.repository

import ru.samlib.client.domain.model.Image

interface IllustrationsRepository {
    suspend fun load(link: String): SamlibResult<List<Image>>
}
