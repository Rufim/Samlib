package ru.samlib.client.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HistoryDaoTest {
    private lateinit var db: SamlibDatabase
    private lateinit var dao: HistoryDao

    private fun entity(
        link: String,
        opened: Long,
        pinned: Boolean = false,
    ) = WorkEntity(
        link = link,
        title = link,
        authorLink = null,
        updateDate = null,
        size = null,
        openedDate = opened,
        authorShortName = "A",
        genres = "G",
        pinned = pinned,
    )

    @Before
    fun setup() {
        db =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    SamlibDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        dao = db.historyDao()
    }

    @After
    fun teardown() = db.close()

    @Test
    fun `сортировка pinned-first затем по дате`() =
        runTest {
            dao.upsert(entity("/a", 100))
            dao.upsert(entity("/b", 200))
            dao.upsert(entity("/c", 50, pinned = true))
            val list = dao.observeHistory().first()
            assertEquals(listOf("/c", "/b", "/a"), list.map { it.link })
        }

    @Test
    fun `clearUnpinned оставляет закреплённые`() =
        runTest {
            dao.upsert(entity("/a", 100))
            dao.upsert(entity("/c", 50, pinned = true))
            dao.clearUnpinned()
            val list = dao.observeHistory().first()
            assertEquals(listOf("/c"), list.map { it.link })
        }

    @Test
    fun `delete и setPinned и isPinned`() =
        runTest {
            dao.upsert(entity("/a", 100))
            dao.setPinned("/a", true)
            assertEquals(true, dao.isPinned("/a"))
            dao.delete("/a")
            assertTrue(dao.observeHistory().first().isEmpty())
        }
}
