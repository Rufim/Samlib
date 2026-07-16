package ru.samlib.client.data.testfixtures

import ru.samlib.client.data.db.CachedWorkDao
import ru.samlib.client.data.db.CachedWorkEntity

class FakeCachedWorkDao : CachedWorkDao {
    private val store = mutableMapOf<String, CachedWorkEntity>()

    override suspend fun upsert(work: CachedWorkEntity) {
        store[work.link] = work
    }

    override suspend fun get(link: String): CachedWorkEntity? = store[link]

    override suspend fun deleteOlderThan(cutoff: Long) {
        store.values.removeAll { it.savedDate < cutoff }
    }

    override suspend fun clear() = store.clear()
}
