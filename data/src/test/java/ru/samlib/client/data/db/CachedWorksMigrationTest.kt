package ru.samlib.client.data.db

import androidx.room.Room
import androidx.room.util.TableInfo
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CachedWorksMigrationTest {
    private lateinit var db: SamlibDatabase

    @Before
    fun setUp() {
        db =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    SamlibDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `SQL миграции 5-6 воссоздаёт таблицу идентично ожиданиям Room`() =
        runTest {
            val support = db.openHelper.writableDatabase
            // эталон — таблица, созданная самим Room по сущности CachedWorkEntity (v6)
            val expected = TableInfo.read(support, "cached_works")

            // воспроизводим действие MIGRATION_5_6 и сверяем результат со схемой Room
            support.execSQL("DROP TABLE IF EXISTS `cached_works`")
            support.execSQL(SQL_CREATE_CACHED_WORKS_V6)
            val actual = TableInfo.read(support, "cached_works")

            assertEquals(expected, actual)
        }

    @Test
    fun `после пересоздания cached_works запись сохраняется и читается`() =
        runTest {
            val support = db.openHelper.writableDatabase
            support.execSQL("DROP TABLE IF EXISTS `cached_works`")
            support.execSQL(SQL_CREATE_CACHED_WORKS_V6)

            val entity =
                CachedWorkEntity(
                    link = "/a/b/c.shtml",
                    title = "Книга",
                    authorLink = "/a/b/",
                    authorShortName = "Автор",
                    categoryTitle = "Проза",
                    imageLink = null,
                    size = 42,
                    sizeDiff = 0,
                    rate = null,
                    votes = null,
                    expertRate = null,
                    expertVotes = null,
                    commentCount = 3,
                    customGenres = "Проза",
                    type = "OTHER",
                    annotation = null,
                    description = null,
                    createDate = null,
                    updateDate = null,
                    dateText = null,
                    changedDate = null,
                    genres = emptyList(),
                    annotationBlocks = emptyList(),
                    filePath = "/data/cached/1.html",
                    savedDate = 100,
                )
            db.cachedWorkDao().upsert(entity)
            assertEquals(entity, db.cachedWorkDao().get("/a/b/c.shtml"))
        }
}
