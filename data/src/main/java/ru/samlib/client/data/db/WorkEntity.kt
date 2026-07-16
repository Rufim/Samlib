package ru.samlib.client.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "works")
data class WorkEntity(
    @PrimaryKey val link: String,
    val title: String?,
    val authorLink: String?,
    val updateDate: Long?,
    val size: Int?,
    val openedDate: Long = 0,
    val authorShortName: String? = null,
    val genres: String? = null,
    val pinned: Boolean = false,
)
