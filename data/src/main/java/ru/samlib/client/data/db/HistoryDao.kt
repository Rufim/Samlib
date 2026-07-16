package ru.samlib.client.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(work: WorkEntity)

    @Query("SELECT * FROM works WHERE openedDate > 0 ORDER BY pinned DESC, openedDate DESC LIMIT :limit")
    fun observeHistory(limit: Int = 500): Flow<List<WorkEntity>>

    @Query("DELETE FROM works WHERE openedDate > 0 AND pinned = 0")
    suspend fun clearUnpinned()

    @Query("DELETE FROM works WHERE link = :link")
    suspend fun delete(link: String)

    @Query("UPDATE works SET pinned = :pinned WHERE link = :link")
    suspend fun setPinned(
        link: String,
        pinned: Boolean,
    )

    @Query("SELECT pinned FROM works WHERE link = :link")
    suspend fun isPinned(link: String): Boolean?
}
