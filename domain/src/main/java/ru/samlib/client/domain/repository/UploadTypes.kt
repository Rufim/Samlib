package ru.samlib.client.domain.repository

/** Данные аккаунта samlib для префилла формы комментария. */
data class AccountInfo(
    val name: String,
    val email: String,
    val url: String,
)

/** Файл с телом произведения для загрузки в форму new_book/edit_book. */
data class UploadFile(
    val name: String,
    val bytes: ByteArray,
    val mime: String,
)

/** Изображение для multipart-загрузки: сырые байты и MIME-тип. */
data class ImageUpload(
    val bytes: ByteArray, // содержимое файла
    val mime: String, // MIME, напр. "image/jpeg"
)
