package ru.samlib.client.domain.repository

/** Вариант выбора в select формы редактора: значение и подпись без транслита. */
data class SelectOption(
    val value: String,
    val label: String,
)

/** Форма «Сведения об авторе»: каталог, все поля формы и текст «о себе». */
data class AboutForm(
    val dir: String,
    val fields: Map<String, String>,
    val body: String,
)

/** Форма «Сведения о разделе» (edit_dir): все поля формы + удобные редактируемые. */
data class DirForm(
    val dir: String,
    val fields: Map<String, String>,
    val author: String,
    val title: String,
    val email: String,
    val homeLink: String,
    val annotation: String,
)

/** Редактируемые поля произведения (new_book/edit_book): значения из формы читалки. */
data class WorkEditFields(
    val title: String, // заголовок произведения
    val type: String, // TYPE — тип (рассказ/повесть/…)
    val janr: String, // JANR — жанр
    val group: String, // ADD_TYPE — раздел (группа) автора
    val annotation: String, // ANNOT — аннотация
    val text: String, // тело произведения; пусто, если прикреплён файл
)

/** Редактируемые поля «Сведений о разделе» (edit_dir). */
data class DirFields(
    val author: String, // AUTHOR — имя автора
    val title: String, // TITLE — название раздела
    val email: String, // EMAIL — контактный e-mail
    val homeLink: String, // HOME_LINK — ссылка на домашнюю страницу
    val annotation: String, // ANNOT — аннотация раздела
)

/** Одна группа (раздел) автора: название и аннотация. */
data class GroupEntry(
    val title: String,
    val annotation: String,
)

/** Форма групп (/cgi-bin/groupedit): скрытые поля + слоты групп (максимум [max]). */
data class GroupForm(
    val fields: Map<String, String>,
    val max: Int,
    val groups: List<GroupEntry>,
)

/** Форма нового произведения (new_book): дефолты полей и варианты селектов. */
data class NewWorkForm(
    val dir: String,
    val fields: Map<String, String>,
    val author: String,
    val date: String,
    val types: List<SelectOption>,
    val janrs: List<SelectOption>,
    val groups: List<SelectOption>,
)

/**
 * Форма редактирования произведения (edit_book + текст из «(Text edit)»).
 * [meta] — поля формы метаданных в порядке формы, [text] — текущее тело,
 * [textFields] — скрытые поля формы «(Text edit)» для сохранения тела.
 */
data class WorkEditForm(
    val dir: String,
    val file: String,
    val meta: NewWorkForm,
    val text: String,
    val textFields: Map<String, String>,
)

/** Редактирование личного раздела автора через /cgi-bin/zhurnal (нужна сессия). */
interface AuthorEditRepository {
    suspend fun loadAbout(authorLink: String): SamlibResult<AboutForm>

    suspend fun loadGroups(authorLink: String): SamlibResult<GroupForm>

    suspend fun saveGroups(
        form: GroupForm,
        groups: List<GroupEntry>,
    ): SamlibResult<Unit>

    suspend fun uploadAvatar(
        authorLink: String,
        image: ImageUpload,
        slot: String = ".photo2",
    ): SamlibResult<Unit>

    suspend fun loadDir(authorLink: String): SamlibResult<DirForm>

    suspend fun saveDir(
        form: DirForm,
        fields: DirFields,
    ): SamlibResult<Unit>

    suspend fun loadNewWork(authorLink: String): SamlibResult<NewWorkForm>

    suspend fun createWork(
        form: NewWorkForm,
        fileName: String,
        fields: WorkEditFields,
        file: UploadFile? = null,
    ): SamlibResult<Unit>

    suspend fun loadWorkEdit(
        authorLink: String,
        file: String,
    ): SamlibResult<WorkEditForm>

    suspend fun saveWorkEdit(
        form: WorkEditForm,
        fields: WorkEditFields,
    ): SamlibResult<Unit>

    suspend fun deleteWork(
        authorLink: String,
        file: String,
    ): SamlibResult<Unit>

    suspend fun saveAbout(
        form: AboutForm,
        body: String,
    ): SamlibResult<Unit>
}
