package ru.samlib.client.data.repository

import ru.samlib.client.data.parser.CommentsParser
import ru.samlib.client.data.parser.ParseException
import ru.samlib.client.data.parser.RatingPager
import ru.samlib.client.data.remote.SamlibHttpClient
import ru.samlib.client.data.remote.samlibCatching
import ru.samlib.client.domain.model.CommentsPage
import ru.samlib.client.domain.repository.CommentDraft
import ru.samlib.client.domain.repository.CommentsRepository
import ru.samlib.client.domain.repository.SamlibResult
import ru.samlib.client.domain.samlib.SamlibLinkFormat

class DefaultCommentsRepository(
    private val http: SamlibHttpClient,
    private val parser: CommentsParser,
) : CommentsRepository {
    override suspend fun load(
        link: String,
        page: Int,
        loginCookie: String,
    ): SamlibResult<CommentsPage> =
        samlibCatching {
            val sep = if (link.contains("?")) "&" else "?"
            val html = http.getPage("$link${sep}PAGE=${page + 1}", loginCookie)
            val comments = parser.parse(html)
            val totalPages = RatingPager.parsePageCount(html)
            val hasMore = totalPages != null && page + 1 < totalPages
            CommentsPage(comments = comments, hasMore = hasMore)
        }

    override suspend fun post(
        commentsLink: String,
        draft: CommentDraft,
        loginCookie: String,
    ): SamlibResult<Unit> =
        samlibCatching {
            val workNoSuffix =
                commentsLink
                    .removePrefix(SamlibLinkFormat.COMMENT_PREFIX)
                    .removeSuffix(SamlibLinkFormat.WORK_SUFFIX)
            val error =
                http.postComment(
                    workLinkNoSuffix = workNoSuffix,
                    draft = draft,
                    operation = if (draft.msgid.isNullOrBlank()) "store_new" else "store_reply",
                    loginCookie = loginCookie,
                )
            if (error != null) throw ParseException(error)
        }

    override suspend fun delete(
        commentsLink: String,
        msgid: String,
        loginCookie: String,
    ): SamlibResult<Unit> =
        samlibCatching {
            val ok = http.commentAction(commentsLink, CommentsParser.OPERATION_DELETE, msgid, loginCookie)
            if (!ok) throw ParseException("Не удалось удалить комментарий")
        }
}
