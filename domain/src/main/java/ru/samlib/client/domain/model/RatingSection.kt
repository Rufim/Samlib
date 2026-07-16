package ru.samlib.client.domain.model

/**
 * Сетевые разделы-списки бокового меню (рейтинговые/каталожные страницы samlib.ru).
 * Каждый раздел знает свой заголовок, базовый путь и стратегию пагинации.
 */
enum class RatingSection(
    val title: String,
    val path: String,
    val paging: Paging,
) {
    TOP_AUTHORS("Топ авторов", "/rating/hits/", Paging.INDEX),
    REVIEWS("Обзоры", "/janr/index_janr_23", Paging.GENRE),
    DISCUSSIONS("Обсуждения", "/rating/comment/", Paging.INDEX),
    FAVORITES("Избранное", "/rating/expert/", Paging.INDEX),
    ;

    enum class Paging { INDEX, GENRE }
}
