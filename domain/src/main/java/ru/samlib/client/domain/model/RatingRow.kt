package ru.samlib.client.domain.model

/** Строка раздела-списка: автор, произведение или обсуждение. */
sealed interface RatingRow {
    data class AuthorItem(
        val author: Author,
    ) : RatingRow

    data class WorkItem(
        val work: Work,
    ) : RatingRow

    data class DiscussionItem(
        val discussion: Discussion,
    ) : RatingRow
}

/** Одна загруженная страница раздела. */
data class RatingPage(
    val rows: List<RatingRow>,
    val hasMore: Boolean,
)
