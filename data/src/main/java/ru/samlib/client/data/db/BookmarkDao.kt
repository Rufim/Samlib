package ru.samlib.client.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(bookmark: BookmarkEntity)

    @Query("SELECT * FROM bookmarks WHERE workLink = :workLink ORDER BY savedDate DESC")
    fun observeForWork(workLink: String): Flow<List<BookmarkEntity>>

    @Query(
        "SELECT indent FROM bookmarks WHERE workLink = :workLink AND userBookmark = 0 ORDER BY savedDate DESC LIMIT 1",
    )
    suspend fun readingPosition(workLink: String): Int?

    @Query("DELETE FROM bookmarks WHERE workLink = :workLink AND userBookmark = 0")
    suspend fun clearAuto(workLink: String)
}
