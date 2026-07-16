package ru.samlib.client.domain.model

/**
 * Книга под наблюдением: работа отслеживаемого автора либо отдельно
 * отслеживаемая (standalone). excluded — исключена из отслеживания
 * у отслеживаемого автора (подписка на автора сохраняется).
 *
 * Стабильная для рекомпозиции копия — `ru.samlib.client.ui.home.UiObservedWork` (`:ui`).
 */
data class ObservedWork(
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
