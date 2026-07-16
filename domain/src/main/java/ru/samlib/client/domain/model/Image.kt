package ru.samlib.client.domain.model

/** Иллюстрация к произведению (страница `/img/.../index.shtml`). */
data class Image(
    val number: Int,
    val url: String,
    val title: String? = null,
    val annotation: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val sizeKb: Int? = null,
)
