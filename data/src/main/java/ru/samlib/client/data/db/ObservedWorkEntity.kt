package ru.samlib.client.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Книга под наблюдением: работа отслеживаемого автора либо отдельно
 * отслеживаемая книга (isStandalone). excluded — исключена из отслеживания
 * у отслеживаемого автора (подписка на автора сохраняется).
 */
@Entity(tableName = "observed_works")
data class ObservedWorkEntity(
    @PrimaryKey val link: String,
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
