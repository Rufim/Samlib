package ru.samlib.client.data.testfixtures

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import ru.samlib.client.data.db.ObservedWorkDao
import ru.samlib.client.data.db.ObservedWorkEntity

class FakeObservedWorkDao : ObservedWorkDao {
    private val map = linkedMapOf<String, ObservedWorkEntity>()
    private val state = MutableStateFlow<List<ObservedWorkEntity>>(emptyList())

    private fun publish() {
        state.value = map.values.toList()
    }

    override suspend fun upsert(work: ObservedWorkEntity) {
        map[work.link] = work
        publish()
    }

    override suspend fun upsertAll(works: List<ObservedWorkEntity>) {
        works.forEach { map[it.link] = it }
        publish()
    }

    override suspend fun get(link: String): ObservedWorkEntity? = map[link]

    override fun observe(link: String): Flow<ObservedWorkEntity?> =
        state.map { list -> list.firstOrNull { it.link == link } }

    override suspend fun getByAuthor(authorLink: String): List<ObservedWorkEntity> =
        map.values.filter { it.authorLink == authorLink }

    override suspend fun getStandalone(): List<ObservedWorkEntity> =
        map.values.filter { it.isStandalone && !it.excluded }

    override fun observeStandalone(): Flow<List<ObservedWorkEntity>> =
        state.map { list -> list.filter { it.isStandalone && !it.excluded } }

    override fun observeByAuthor(authorLink: String): Flow<List<ObservedWorkEntity>> =
        state.map { list -> list.filter { it.authorLink == authorLink && !it.excluded } }

    override suspend fun delete(link: String) {
        map.remove(link)
        publish()
    }

    override suspend fun deleteByAuthor(authorLink: String) {
        map.values.removeAll { it.authorLink == authorLink && !it.isStandalone }
        publish()
    }

    override suspend fun clearChanged(link: String) {
        map[link]?.let { map[link] = it.copy(changed = false, sizeDiff = 0) }
        publish()
    }

    override suspend fun clearChangedByAuthor(authorLink: String) {
        map.values
            .filter { it.authorLink == authorLink }
            .forEach { map[it.link] = it.copy(changed = false, sizeDiff = 0) }
        publish()
    }

    override suspend fun countChangedByAuthor(authorLink: String): Int =
        map.values.count { it.authorLink == authorLink && it.changed && !it.excluded }
}
