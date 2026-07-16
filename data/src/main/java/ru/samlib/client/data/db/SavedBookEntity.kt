package ru.samlib.client.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/** Книга, открытая извне (файл), с адаптированными метаданными из fb2/epub-тегов. */
@Entity(tableName = "saved_books")
data class SavedBookEntity(
    @PrimaryKey val uri: String,
    val title: String,
    val author: String?,
    val format: String?,
    val dateAdded: Long,
)

@Dao
interface SavedBookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(book: SavedBookEntity)

    @Query("SELECT * FROM saved_books ORDER BY dateAdded DESC")
    suspend fun getAll(): List<SavedBookEntity>

    @Query("DELETE FROM saved_books WHERE uri = :uri")
    suspend fun delete(uri: String)
}
