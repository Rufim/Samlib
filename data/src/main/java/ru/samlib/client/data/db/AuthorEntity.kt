package ru.samlib.client.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "authors")
data class AuthorEntity(
    @PrimaryKey val link: String,
    val fullName: String?,
    val shortName: String?,
    val lastUpdateDate: Long?,
    val lastCheckedDate: Long?,
    val workCount: Int?,
    val isObservable: Boolean,
    val hasUpdates: Boolean = false,
    val notNotified: Boolean = false,
)
