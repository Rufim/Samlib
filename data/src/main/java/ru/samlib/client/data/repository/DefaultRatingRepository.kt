package ru.samlib.client.data.repository

import ru.samlib.client.data.parser.DiscussionParser
import ru.samlib.client.data.parser.RatingPager
import ru.samlib.client.data.parser.TopAuthorsParser
import ru.samlib.client.data.parser.WorkRatingParser
import ru.samlib.client.data.remote.SamlibHttpClient
import ru.samlib.client.data.remote.samlibCatching
import ru.samlib.client.domain.model.RatingPage
import ru.samlib.client.domain.model.RatingRow
import ru.samlib.client.domain.model.RatingSection
import ru.samlib.client.domain.repository.RatingRepository
import ru.samlib.client.domain.repository.SamlibResult

class DefaultRatingRepository(
    private val http: SamlibHttpClient,
    private val topAuthorsParser: TopAuthorsParser,
    private val workRatingParser: WorkRatingParser,
    private val discussionParser: DiscussionParser,
) : RatingRepository {
    override suspend fun load(
        section: RatingSection,
        page: Int,
    ): SamlibResult<RatingPage> =
        samlibCatching {
            val html = http.getPage(RatingPager.pageUrl(section, page))
            val rows: List<RatingRow> =
                when (section) {
                    RatingSection.TOP_AUTHORS ->
                        topAuthorsParser.parse(html).map { RatingRow.AuthorItem(it) }

                    RatingSection.REVIEWS, RatingSection.FAVORITES ->
                        workRatingParser.parse(html).map { RatingRow.WorkItem(it) }

                    RatingSection.DISCUSSIONS ->
                        discussionParser.parse(html).map { RatingRow.DiscussionItem(it) }
                }
            val totalPages = RatingPager.parsePageCount(html)
            val hasMore = totalPages != null && page + 1 < totalPages
            RatingPage(rows = rows, hasMore = hasMore)
        }
}
