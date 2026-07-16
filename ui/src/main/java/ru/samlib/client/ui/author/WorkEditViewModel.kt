package ru.samlib.client.ui.author

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import ru.samlib.client.domain.repository.AuthorEditRepository
import ru.samlib.client.domain.repository.DraftBlock
import ru.samlib.client.domain.repository.NewWorkForm
import ru.samlib.client.domain.repository.SamlibResult
import ru.samlib.client.domain.repository.SelectOption
import ru.samlib.client.domain.repository.WorkDraftChunker
import ru.samlib.client.domain.repository.WorkEditFields
import ru.samlib.client.domain.repository.WorkEditForm
import ru.samlib.client.domain.repository.errorText
import ru.samlib.client.domain.samlib.SamlibLinkFormat
import ru.samlib.client.domain.usecase.LoadWorkEditUseCase
import ru.samlib.client.domain.usecase.SaveWorkEditUseCase
import ru.samlib.client.domain.usecase.WorkFormError
import ru.samlib.client.domain.usecase.WorkFormValidator
import ru.samlib.client.ui.common.EditSideEffect

@Immutable
data class WorkEditState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val loaded: Boolean = false,
    val isEdit: Boolean = false,
    val fileName: String = "",
    val title: String = "",
    val annotation: String = "",
    val text: String = "",
    val types: List<SelectOption> = emptyList(),
    val janrs: List<SelectOption> = emptyList(),
    val groups: List<SelectOption> = emptyList(),
    val type: String = "",
    val janr: String = "",
    val group: String = "",
    /** Имя прикреплённого файла с телом (только при создании); null — тело из текстового поля. */
    val attachedFileName: String? = null,
    val error: String? = null,
    val draftBlocks: List<DraftBlock> = emptyList(),
    val chunkingDraft: Boolean = false,
)

/** Создание ([workFile] == null) или редактирование произведения в своём разделе. */
class WorkEditViewModel(
    private val authorLink: String,
    private val workFile: String?,
    private val repo: AuthorEditRepository,
    private val chunker: WorkDraftChunker,
    private val loadWorkEdit: LoadWorkEditUseCase,
    private val saveWorkEdit: SaveWorkEditUseCase,
) : ViewModel(),
    ContainerHost<WorkEditState, EditSideEffect> {
    override val container =
        viewModelScope.container<WorkEditState, EditSideEffect>(WorkEditState(isEdit = workFile != null))

    private var newForm: NewWorkForm? = null
    private var editForm: WorkEditForm? = null
    private var attachedBytes: ByteArray? = null
    private var attachedMime: String = "text/plain"

    /** Чанкинг (парсинг Jsoup по кускам) — тяжёлая операция на объёмном тексте, считаем в фоне. */
    private suspend fun chunkText(text: String): List<DraftBlock> =
        withContext(Dispatchers.Default) { chunker.chunk(text) }

    fun load() =
        intent {
            reduce { state.copy(loading = true, error = null) }
            when (val result = loadWorkEdit(authorLink, workFile)) {
                is LoadWorkEditUseCase.Result.New -> {
                    newForm = result.form
                    val f = result.form.fields
                    reduce {
                        state.copy(
                            loading = false,
                            loaded = true,
                            types = result.form.types,
                            janrs = result.form.janrs,
                            groups = result.form.groups,
                            type = f["TYPE"].orEmpty(),
                            janr = f["JANR"].orEmpty(),
                            group = f["ADD_TYPE"].orEmpty(),
                        )
                    }
                }

                is LoadWorkEditUseCase.Result.Existing -> {
                    editForm = result.form
                    val f = result.form.meta.fields
                    reduce {
                        state.copy(
                            loading = false,
                            loaded = true,
                            fileName = workFile.orEmpty(),
                            title = f["TITLE"].orEmpty(),
                            annotation = f["ANNOT"].orEmpty(),
                            text = result.form.text,
                            types = result.form.meta.types,
                            janrs = result.form.meta.janrs,
                            groups = result.form.meta.groups,
                            type = f["TYPE"].orEmpty(),
                            janr = f["JANR"].orEmpty(),
                            group = f["ADD_TYPE"].orEmpty(),
                            draftBlocks = result.draftBlocks,
                        )
                    }
                }

                is LoadWorkEditUseCase.Result.Failed ->
                    reduce { state.copy(loading = false, error = result.message) }
            }
        }

    fun setFileName(v: String) = intent { reduce { state.copy(fileName = v) } }

    fun setTitle(v: String) = intent { reduce { state.copy(title = v) } }

    fun setAnnotation(v: String) = intent { reduce { state.copy(annotation = v) } }

    fun setText(v: String) =
        intent {
            reduce { state.copy(text = v, chunkingDraft = true) }
            val blocks = chunkText(v)
            reduce { state.copy(draftBlocks = blocks, chunkingDraft = false) }
        }

    fun setType(v: String) = intent { reduce { state.copy(type = v) } }

    fun setJanr(v: String) = intent { reduce { state.copy(janr = v) } }

    fun setGroup(v: String) = intent { reduce { state.copy(group = v) } }

    fun attachFile(
        name: String,
        bytes: ByteArray,
        mime: String?,
    ) = intent {
        attachedBytes = bytes
        attachedMime = mime ?: "text/plain"
        reduce { state.copy(attachedFileName = name) }
    }

    fun detachFile() =
        intent {
            attachedBytes = null
            reduce { state.copy(attachedFileName = null) }
        }

    /** Тексты ошибок валидации формы (правила — в WorkFormValidator). */
    private fun WorkFormError.message(): String =
        when (this) {
            WorkFormError.EmptyTitle -> "Укажите название"
            WorkFormError.BadFileName -> "Имя файла — латиница, цифры, «-», «_»"
            WorkFormError.TextTooShort ->
                "Текст слишком короткий: сервер требует минимум ${WorkFormValidator.MIN_BODY_BYTES} символов"
        }

    fun save() =
        intent {
            if (state.saving) return@intent
            val target =
                if (workFile == null) {
                    val form = newForm ?: return@intent
                    val fileName = state.fileName.trim().removeSuffix(SamlibLinkFormat.WORK_SUFFIX)
                    SaveWorkEditUseCase.Target.New(form, fileName)
                } else {
                    val form = editForm ?: return@intent
                    SaveWorkEditUseCase.Target.Existing(form)
                }
            val attached = attachedBytes
            val params =
                SaveWorkEditUseCase.Params(
                    target = target,
                    fields =
                        WorkEditFields(
                            title = state.title.trim(),
                            type = state.type,
                            janr = state.janr,
                            group = state.group,
                            annotation = state.annotation.trim(),
                            text = if (attached == null) state.text else "",
                        ),
                    attachedBytes = attached,
                    attachedFileName = state.attachedFileName,
                    attachedMime = attachedMime,
                )
            saveWorkEdit.validate(params)?.let { error ->
                postSideEffect(EditSideEffect.Error(error.message()))
                return@intent
            }
            reduce { state.copy(saving = true) }
            val result = saveWorkEdit(params)
            reduce { state.copy(saving = false) }
            when (result) {
                is SaveWorkEditUseCase.Result.Saved -> postSideEffect(EditSideEffect.Saved)
                is SaveWorkEditUseCase.Result.Failed -> postSideEffect(EditSideEffect.Error(result.message))
            }
        }

    fun delete() =
        intent {
            val file = workFile ?: return@intent
            if (state.saving) return@intent
            reduce { state.copy(saving = true) }
            val result = repo.deleteWork(authorLink, file)
            reduce { state.copy(saving = false) }
            when (result) {
                is SamlibResult.Success -> postSideEffect(EditSideEffect.Deleted)
                is SamlibResult.Error ->
                    postSideEffect(
                        EditSideEffect.Error(
                            result.cause?.message ?: result.type.errorText(),
                        ),
                    )
            }
        }
}
