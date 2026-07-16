package ru.samlib.client.domain.repository

import ru.samlib.client.domain.model.RatingPage
import ru.samlib.client.domain.model.RatingRow
import ru.samlib.client.domain.model.RatingSection

/**
 * Грузит страницу раздела-списка: строит URL, тянет HTML, вызывает нужный
 * парсер и оборачивает результат в [RatingRow]. Количество страниц берётся из
 * маркера `Страниц (N)`; если маркера нет — список считается одностраничным.
 */
interface RatingRepository {
    suspend fun load(
        section: RatingSection,
        page: Int,
    ): SamlibResult<RatingPage>
}
