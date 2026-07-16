package ru.samlib.client.domain.model

/** Комментарий к произведению (страница `/comment/...`). */
data class Comment(
    val number: Int,
    val nickName: String? = null,
    val date: String? = null,
    /** Полный текст: строки-цитаты начинаются с «>» (глубина = число «>»), остальное — свой текст. */
    val body: String = "",
    val userLink: String? = null,
    val deleted: Boolean = false,
    /** Номер процитированного комментария (для подписи цитаты), либо null. */
    val parentNumber: Int? = null,
    /** MSGID для действий (ответ/удаление) — доступен авторизованному пользователю. */
    val msgid: String? = null,
    val canReply: Boolean = false,
    val canDelete: Boolean = false,
)

/** Одна загруженная страница комментариев. */
data class CommentsPage(
    val comments: List<Comment>,
    val hasMore: Boolean,
)
