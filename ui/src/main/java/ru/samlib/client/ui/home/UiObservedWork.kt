package ru.samlib.client.ui.home

import androidx.compose.runtime.Immutable
import ru.samlib.client.domain.model.ObservedWork

/** Стабильная для Compose-рекомпозиции копия [ObservedWork]. */
@Immutable
data class UiObservedWork(
    val link: String,
    val authorLink: String,
    val authorName: String? = null,
    val title: String? = null,
    val categoryTitle: String? = null,
    val size: Int = 0,
    val updateDate: Long? = null,
    val changed: Boolean = false,
    val sizeDiff: Int = 0,
    val changedDate: Long? = null,
    val isStandalone: Boolean = false,
    val excluded: Boolean = false,
)

fun ObservedWork.toUi(): UiObservedWork =
    UiObservedWork(
        link = link,
        authorLink = authorLink,
        authorName = authorName,
        title = title,
        categoryTitle = categoryTitle,
        size = size,
        updateDate = updateDate,
        changed = changed,
        sizeDiff = sizeDiff,
        changedDate = changedDate,
        isStandalone = isStandalone,
        excluded = excluded,
    )

fun UiObservedWork.toDomain(): ObservedWork =
    ObservedWork(
        link = link,
        authorLink = authorLink,
        authorName = authorName,
        title = title,
        categoryTitle = categoryTitle,
        size = size,
        updateDate = updateDate,
        changed = changed,
        sizeDiff = sizeDiff,
        changedDate = changedDate,
        isStandalone = isStandalone,
        excluded = excluded,
    )

fun List<ObservedWork>.toUi(): List<UiObservedWork> = map { it.toUi() }

fun List<UiObservedWork>.toDomain(): List<ObservedWork> = map { it.toDomain() }
