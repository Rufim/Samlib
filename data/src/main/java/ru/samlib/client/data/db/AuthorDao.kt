package ru.samlib.client.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AuthorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(author: AuthorEntity)

    @Query("DELETE FROM authors WHERE link = :link")
    suspend fun delete(link: String)

    @Query("SELECT * FROM authors WHERE link = :link")
    suspend fun get(link: String): AuthorEntity?

    @Query("SELECT * FROM authors WHERE isObservable = 1 ORDER BY fullName")
    fun observeObservable(): Flow<List<AuthorEntity>>

    @Query("UPDATE authors SET hasUpdates = :hasUpdates, notNotified = :notNotified WHERE link = :link")
    suspend fun setUpdateFlags(
        link: String,
        hasUpdates: Boolean,
        notNotified: Boolean,
    )

    @Query("UPDATE authors SET notNotified = 0 WHERE link IN (:links)")
    suspend fun clearNotNotified(links: List<String>)
}
