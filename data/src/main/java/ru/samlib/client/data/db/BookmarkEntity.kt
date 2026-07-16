package ru.samlib.client.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workLink: String,
    val indent: Int,
    val savedDate: Long,
    val userBookmark: Boolean,
)
