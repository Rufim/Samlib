package ru.samlib.client.data.testfixtures

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import ru.samlib.client.data.db.BookmarkDao
import ru.samlib.client.data.db.BookmarkEntity

class FakeBookmarkDao : BookmarkDao {
    private val items = mutableListOf<BookmarkEntity>()

    override suspend fun upsert(bookmark: BookmarkEntity) {
        items.add(bookmark)
    }

    override fun observeForWork(workLink: String): Flow<List<BookmarkEntity>> =
        flowOf(items.filter { it.workLink == workLink })

    override suspend fun readingPosition(workLink: String): Int? =
        items
            .filter { it.workLink == workLink && !it.userBookmark }
            .maxByOrNull { it.savedDate }
            ?.indent

    override suspend fun clearAuto(workLink: String) {
        items.removeAll { it.workLink == workLink && !it.userBookmark }
    }
}
