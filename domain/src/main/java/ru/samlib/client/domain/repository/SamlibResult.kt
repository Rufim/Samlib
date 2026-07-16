package ru.samlib.client.domain.repository

enum class SamlibError { NETWORK, PARSE, NOT_FOUND, UNKNOWN }

sealed interface SamlibResult<out T> {
    data class Success<T>(
        val data: T,
    ) : SamlibResult<T>

    data class Error(
        val type: SamlibError,
        val cause: Throwable? = null,
    ) : SamlibResult<Nothing>
}

fun SamlibError.errorText(): String =
    when (this) {
        SamlibError.NETWORK -> "Нет сети или сервер недоступен"
        SamlibError.PARSE -> "Не удалось разобрать страницу"
        SamlibError.NOT_FOUND -> "Страница не найдена"
        SamlibError.UNKNOWN -> "Неизвестная ошибка"
    }
