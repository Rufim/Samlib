package ru.samlib.client.domain.repository

import ru.samlib.client.domain.model.ContentBlock

data class LocalDocument(
    val title: String,
    val blocks: List<ContentBlock>,
    val author: String? = null,
    /** fb2 | fb2.zip | epub | docx | doc | html | txt — для маршрутизации и имени копии. */
    val format: String = "txt",
)

interface LocalFileRepository {
    suspend fun load(uri: String): LocalDocument

    /** Есть ли постоянный доступ к [uri] (SAF-разрешение) — переживёт перезапуск приложения. */
    fun hasPersistedAccess(uri: String): Boolean

    /**
     * Устойчивый URI для повторных открытий: content:// с разовым доступом (VIEW-интент)
     * копируется в файлы приложения и возвращается file://-копия; остальные — как есть.
     */
    suspend fun ensureLocalCopy(uri: String): String

    /** Закрепить доступ на чтение [uri], чтобы файл переживал перезапуск приложения (SAF). */
    fun persistReadAccess(uri: String)
}
