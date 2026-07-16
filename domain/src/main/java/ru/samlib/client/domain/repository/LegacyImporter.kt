package ru.samlib.client.domain.repository

import java.io.File

/** Итог переноса legacy-данных: количество перенесённых подписок/истории/сохранённых книг. */
data class LegacyImportReport(
    val subscriptions: Int = 0,
    val history: Int = 0,
    val savedBooks: Int = 0,
) {
    val total: Int get() = subscriptions + history + savedBooks
}

/** Перенос данных из старой (legacy, DBFlow) БД «Samlib.db» в новую Room-БД. */
interface LegacyImporter {
    /** Файл legacy-БД при обновлении поверх (applicationId совпадает) — или null, если его нет. */
    fun defaultLegacyDb(): File?

    suspend fun import(dbFile: File): LegacyImportReport
}
