package ru.samlib.client.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ObservedWorkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(work: ObservedWorkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(works: List<ObservedWorkEntity>)

    @Query("SELECT * FROM observed_works WHERE link = :link")
    suspend fun get(link: String): ObservedWorkEntity?

    @Query("SELECT * FROM observed_works WHERE link = :link")
    fun observe(link: String): Flow<ObservedWorkEntity?>

    @Query("SELECT * FROM observed_works WHERE authorLink = :authorLink")
    suspend fun getByAuthor(authorLink: String): List<ObservedWorkEntity>

    @Query("SELECT * FROM observed_works WHERE isStandalone = 1 AND excluded = 0")
    suspend fun getStandalone(): List<ObservedWorkEntity>

    @Query("SELECT * FROM observed_works WHERE isStandalone = 1 AND excluded = 0 ORDER BY title")
    fun observeStandalone(): Flow<List<ObservedWorkEntity>>

    @Query("SELECT * FROM observed_works WHERE authorLink = :authorLink AND excluded = 0")
    fun observeByAuthor(authorLink: String): Flow<List<ObservedWorkEntity>>

    @Query("DELETE FROM observed_works WHERE link = :link")
    suspend fun delete(link: String)

    @Query("DELETE FROM observed_works WHERE authorLink = :authorLink AND isStandalone = 0")
    suspend fun deleteByAuthor(authorLink: String)

    @Query("UPDATE observed_works SET changed = 0, sizeDiff = 0 WHERE link = :link")
    suspend fun clearChanged(link: String)

    @Query("UPDATE observed_works SET changed = 0, sizeDiff = 0 WHERE authorLink = :authorLink")
    suspend fun clearChangedByAuthor(authorLink: String)

    @Query("SELECT COUNT(*) FROM observed_works WHERE authorLink = :authorLink AND changed = 1 AND excluded = 0")
    suspend fun countChangedByAuthor(authorLink: String): Int
}
