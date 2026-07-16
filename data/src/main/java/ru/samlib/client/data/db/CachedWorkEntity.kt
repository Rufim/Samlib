package ru.samlib.client.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import ru.samlib.client.domain.model.Genre
import java.math.BigDecimal
import java.util.Date

/** Кеш содержимого произведения для офлайн-чтения. */
@Entity(tableName = "cached_works")
data class CachedWorkEntity(
    @PrimaryKey val link: String,
    val title: String?,
    val authorLink: String?,
    val authorShortName: String?,
    val categoryTitle: String?,
    val imageLink: String?,
    val size: Int?,
    val sizeDiff: Int?,
    val rate: BigDecimal?,
    val votes: Int?,
    val expertRate: BigDecimal?,
    val expertVotes: Int?,
    val commentCount: Int?,
    val customGenres: String?,
    val type: String?, // Type можно хранить как строку
    val annotation: String?,
    val description: String?,
    val createDate: Date?,
    val updateDate: Date?,
    val dateText: String?,
    val changedDate: Date?,
    val genres: List<Genre>, // JSON-список жанров
    val annotationBlocks: List<String>, // JSON-список строк
    val filePath: String,
    val savedDate: Long,
)

// CachedWorkDao.kt
@Dao
interface CachedWorkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(work: CachedWorkEntity)

    @Query("SELECT * FROM cached_works WHERE link = :link")
    suspend fun get(link: String): CachedWorkEntity?

    @Query("DELETE FROM cached_works WHERE savedDate < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("DELETE FROM cached_works")
    suspend fun clear()
}
