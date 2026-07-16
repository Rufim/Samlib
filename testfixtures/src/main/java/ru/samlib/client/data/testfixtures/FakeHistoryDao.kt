package ru.samlib.client.data.testfixtures

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import ru.samlib.client.data.db.HistoryDao
import ru.samlib.client.data.db.WorkEntity

class FakeHistoryDao : HistoryDao {
    private val state = MutableStateFlow<List<WorkEntity>>(emptyList())

    override suspend fun upsert(work: WorkEntity) {
        state.value = state.value.filterNot { it.link == work.link } + work
    }

    override fun observeHistory(limit: Int): Flow<List<WorkEntity>> =
        state.map { list ->
            list
                .sortedWith(
                    compareByDescending<WorkEntity> { it.pinned }.thenByDescending { it.openedDate },
                ).take(limit)
        }

    override suspend fun clearUnpinned() {
        state.value = state.value.filter { it.pinned }
    }

    override suspend fun delete(link: String) {
        state.value = state.value.filterNot { it.link == link }
    }

    override suspend fun setPinned(
        link: String,
        pinned: Boolean,
    ) {
        state.value = state.value.map { if (it.link == link) it.copy(pinned = pinned) else it }
    }

    override suspend fun isPinned(link: String): Boolean? = state.value.firstOrNull { it.link == link }?.pinned
}
