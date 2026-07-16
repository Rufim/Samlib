package ru.samlib.client.domain.model

import java.math.BigDecimal
import java.util.Date

data class Author(
    val link: String,
    val fullName: String? = null,
    val shortName: String? = null,
    val email: String? = null,
    val annotation: String? = null,
    val gender: Gender? = null,
    val dateBirth: Date? = null,
    val address: String? = null,
    val authorSiteUrl: String? = null,
    val lastUpdateDate: Date? = null,
    val lastCheckedDate: Long? = null,
    val size: Int? = null,
    val workCount: Int? = null,
    val rate: BigDecimal? = null,
    val votes: Int? = null,
    val views: Int? = null,
    val about: String? = null,
    val sectionAnnotation: String? = null,
    val friends: Int? = null,
    val friendsOf: Int? = null,
    val avatarUrl: String? = null,
    val categories: List<Category> = emptyList(),
    val links: List<Link> = emptyList(),
    val works: List<Work> = emptyList(),
    val recommendations: List<Work> = emptyList(),
    /** Есть непросмотренные обновления (бейдж «new» в списке отслеживаемых). */
    val hasUpdates: Boolean = false,
)

data class Work(
    val link: String,
    val title: String? = null,
    val authorLink: String? = null,
    val authorShortName: String? = null,
    val categoryTitle: String? = null,
    val imageLink: String? = null,
    val size: Int? = null,
    val sizeDiff: Int? = null,
    val rate: BigDecimal? = null,
    val votes: Int? = null,
    val expertRate: BigDecimal? = null,
    val expertVotes: Int? = null,
    val commentCount: Int? = null,
    val genres: List<Genre> = emptyList(),
    val customGenres: String? = null,
    val type: Type = Type.OTHER,
    val annotation: String? = null,
    val annotationBlocks: List<String> = emptyList(),
    val description: String? = null,
    val createDate: Date? = null,
    val updateDate: Date? = null,
    val dateText: String? = null,
    val changedDate: Date? = null,
    val rawContent: String = "",
    val pinned: Boolean = false,
    val openedDate: Long? = null,
)

data class Discussion(
    val work: Work,
    val author: Author,
    val count: Int? = null,
    val countOfDay: Int? = null,
    val lastOne: String? = null,
    /** Ссылка на страницу комментариев (`/comment/.../x`) для экрана обсуждения. */
    val commentsLink: String? = null,
)

data class Category(
    val title: String? = null,
    val link: String? = null,
    val annotation: String? = null,
    val type: Type = Type.OTHER,
    val works: List<Work> = emptyList(),
    val links: List<Link> = emptyList(),
)

data class Link(
    val link: String? = null,
    val title: String? = null,
    val annotation: String? = null,
)

/** Элемент списка «Сохранённые»: книга из реестра (с метаданными) или файл устройства. */
data class SavedItem(
    val uri: String,
    val title: String,
    val author: String? = null,
    /** Время добавления, миллисекунды. */
    val date: Long,
)
