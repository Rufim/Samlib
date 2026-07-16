package ru.samlib.client.domain.usecase

import ru.samlib.client.domain.repository.AuthorEditRepository
import ru.samlib.client.domain.repository.NewWorkForm
import ru.samlib.client.domain.repository.SamlibResult
import ru.samlib.client.domain.repository.UploadFile
import ru.samlib.client.domain.repository.WorkEditFields
import ru.samlib.client.domain.repository.WorkEditForm
import ru.samlib.client.domain.repository.errorText

/**
 * Сохранение произведения: валидирует форму ([WorkFormValidator]) и, если она прошла проверку,
 * создаёт ([AuthorEditRepository.createWork]) или обновляет ([AuthorEditRepository.saveWorkEdit])
 * в зависимости от [Target] — нового произведения или уже существующего.
 */
class SaveWorkEditUseCase(
    private val repo: AuthorEditRepository,
) {
    sealed interface Target {
        data class New(
            val form: NewWorkForm,
            val fileName: String,
        ) : Target

        data class Existing(
            val form: WorkEditForm,
        ) : Target
    }

    data class Params(
        val target: Target,
        val fields: WorkEditFields,
        val attachedBytes: ByteArray?,
        val attachedFileName: String?,
        val attachedMime: String,
    )

    sealed interface Result {
        data object Saved : Result

        data class Failed(
            val message: String,
        ) : Result
    }

    private fun bodyBytes(params: Params): Int =
        when (val target = params.target) {
            is Target.New ->
                params.attachedBytes?.size
                    ?: params.fields.text
                        .toByteArray(charset("windows-1251"))
                        .size
            // при редактировании неизменённый текст не перепроверяем (форма уже принята сервером)
            is Target.Existing ->
                if (params.fields.text != target.form.text) {
                    params.fields.text
                        .toByteArray(charset("windows-1251"))
                        .size
                } else {
                    Int.MAX_VALUE
                }
        }

    /** Синхронная, чистая проверка — вызывать перед [invoke], чтобы не трогать I/O зря. */
    fun validate(params: Params): WorkFormError? {
        val fileName = (params.target as? Target.New)?.fileName
        return WorkFormValidator.validate(fileName, params.fields.title, bodyBytes(params))
    }

    suspend operator fun invoke(params: Params): Result {
        val outcome =
            when (val target = params.target) {
                is Target.New -> {
                    val file =
                        params.attachedBytes?.let {
                            UploadFile(params.attachedFileName ?: "upload.txt", it, params.attachedMime)
                        }
                    repo.createWork(target.form, target.fileName, params.fields, file)
                }

                is Target.Existing -> repo.saveWorkEdit(target.form, params.fields)
            }
        return when (outcome) {
            is SamlibResult.Success -> Result.Saved
            is SamlibResult.Error -> Result.Failed(outcome.cause?.message ?: outcome.type.errorText())
        }
    }
}
