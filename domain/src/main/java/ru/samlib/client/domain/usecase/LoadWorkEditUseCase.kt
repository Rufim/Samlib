package ru.samlib.client.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.samlib.client.domain.repository.AuthorEditRepository
import ru.samlib.client.domain.repository.DraftBlock
import ru.samlib.client.domain.repository.NewWorkForm
import ru.samlib.client.domain.repository.SamlibResult
import ru.samlib.client.domain.repository.WorkDraftChunker
import ru.samlib.client.domain.repository.WorkEditForm
import ru.samlib.client.domain.repository.errorText

/**
 * Загрузка формы произведения: [workFile] == null — форма нового произведения (new_book),
 * иначе — форма редактирования (edit_book + текст), с чанкингом текста для превью черновика.
 */
class LoadWorkEditUseCase(
    private val repo: AuthorEditRepository,
    private val chunker: WorkDraftChunker,
) {
    sealed interface Result {
        data class New(
            val form: NewWorkForm,
        ) : Result

        data class Existing(
            val form: WorkEditForm,
            val draftBlocks: List<DraftBlock>,
        ) : Result

        data class Failed(
            val message: String,
        ) : Result
    }

    suspend operator fun invoke(
        authorLink: String,
        workFile: String?,
    ): Result =
        if (workFile == null) {
            when (val r = repo.loadNewWork(authorLink)) {
                is SamlibResult.Success -> Result.New(r.data)
                is SamlibResult.Error -> Result.Failed(r.type.errorText())
            }
        } else {
            when (val r = repo.loadWorkEdit(authorLink, workFile)) {
                is SamlibResult.Success -> {
                    val blocks = withContext(Dispatchers.Default) { chunker.chunk(r.data.text) }
                    Result.Existing(r.data, blocks)
                }
                is SamlibResult.Error -> Result.Failed(r.type.errorText())
            }
        }
}
