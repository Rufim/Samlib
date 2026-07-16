package ru.samlib.client.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        AuthorEntity::class, WorkEntity::class, BookmarkEntity::class,
        CachedWorkEntity::class, SavedBookEntity::class, ObservedWorkEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class SamlibDatabase : RoomDatabase() {
    abstract fun authorDao(): AuthorDao

    abstract fun bookmarkDao(): BookmarkDao

    abstract fun historyDao(): HistoryDao

    abstract fun cachedWorkDao(): CachedWorkDao

    abstract fun savedBookDao(): SavedBookDao

    abstract fun observedWorkDao(): ObservedWorkDao
}
