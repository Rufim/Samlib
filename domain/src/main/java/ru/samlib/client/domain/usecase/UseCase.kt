package ru.samlib.client.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Общий контракт usecase: единая сигнатура `invoke`, ошибки — через [Result], а не через
 * бросание исключений вызывающему. Не все usecase проекта используют этот контракт — часть
 * из них имеет несколько операций или собственный (более богатый) тип результата (например,
 * `LoadWorkUseCase` возвращает `SamlibResult`), поэтому подходит не всем, см. README.
 */
abstract class UseCase<in P, out R> {
    suspend operator fun invoke(params: P): Result<R> =
        withContext(Dispatchers.Default) { runCatching { execute(params) } }

    protected abstract suspend fun execute(params: P): R
}
