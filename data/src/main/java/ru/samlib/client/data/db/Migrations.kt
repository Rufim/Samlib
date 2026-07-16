package ru.samlib.client.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Схема таблицы кеша произведений (v6). Тело книги вынесено в файл (filePath),
 * в БД — только метаданные: строки Room ограничены CursorWindow (~2 МБ), из-за
 * чего крупные книги не читались из БД. Держим SQL здесь, чтобы миграция и тест
 * миграции опирались на один источник истины.
 */
const val SQL_CREATE_CACHED_WORKS_V6 =
    "CREATE TABLE IF NOT EXISTS `cached_works` (" +
        "`link` TEXT NOT NULL, `title` TEXT, `authorLink` TEXT, `authorShortName` TEXT, " +
        "`categoryTitle` TEXT, `imageLink` TEXT, `size` INTEGER, `sizeDiff` INTEGER, " +
        "`rate` TEXT, `votes` INTEGER, `expertRate` TEXT, `expertVotes` INTEGER, " +
        "`commentCount` INTEGER, `customGenres` TEXT, `type` TEXT, `annotation` TEXT, " +
        "`description` TEXT, `createDate` INTEGER, `updateDate` INTEGER, `dateText` TEXT, " +
        "`changedDate` INTEGER, `genres` TEXT NOT NULL, `annotationBlocks` TEXT NOT NULL, " +
        "`filePath` TEXT NOT NULL, `savedDate` INTEGER NOT NULL, PRIMARY KEY(`link`))"

/** Аддитивная миграция: флаги обновлений у авторов + таблица книг под наблюдением. */
val MIGRATION_4_5 =
    object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE authors ADD COLUMN hasUpdates INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE authors ADD COLUMN notNotified INTEGER NOT NULL DEFAULT 0")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `observed_works` (" +
                    "`link` TEXT NOT NULL, `authorLink` TEXT NOT NULL, `authorName` TEXT, " +
                    "`title` TEXT, `categoryTitle` TEXT, `size` INTEGER NOT NULL, " +
                    "`updateDate` INTEGER, `changed` INTEGER NOT NULL, `sizeDiff` INTEGER NOT NULL, " +
                    "`changedDate` INTEGER, `isStandalone` INTEGER NOT NULL, `excluded` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`link`))",
            )
        }
    }

/**
 * cached_works переехал с колонки rawContent на файловый кеш (filePath) + метаданные.
 * Кеш восстановим (перечитается из сети), поэтому таблицу пересоздаём; остальные
 * таблицы (история, подписки, закладки, observed_works) не трогаем — данные целы.
 */
val MIGRATION_5_6 =
    object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS `cached_works`")
            db.execSQL(SQL_CREATE_CACHED_WORKS_V6)
        }
    }

/** Все миграции БД в порядке версий. */
val ALL_MIGRATIONS = arrayOf(MIGRATION_4_5, MIGRATION_5_6)
