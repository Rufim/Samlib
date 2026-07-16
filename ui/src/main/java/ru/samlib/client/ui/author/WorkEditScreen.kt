package ru.samlib.client.ui.author

import android.provider.OpenableColumns
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.widget.EditText
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.text.PrecomputedTextCompat
import androidx.core.widget.NestedScrollView
import androidx.core.widget.TextViewCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import org.orbitmvi.orbit.compose.collectAsState
import ru.samlib.client.domain.repository.SelectOption
import ru.samlib.client.ui.R
import ru.samlib.client.ui.common.CollectEditEffects
import ru.samlib.client.ui.common.ConfirmDialog
import ru.samlib.client.ui.common.ErrorBox
import ru.samlib.client.ui.common.FormatTarget
import ru.samlib.client.ui.common.FormatToolbar
import ru.samlib.client.ui.common.LoadingBox
import ru.samlib.client.ui.common.TextPositionDialog
import ru.samlib.client.ui.theme.AppColors

/** Тексты длиннее этого — раскладка считается в фоне, чтобы setText() не фризил UI. */
private const val PRECOMPUTE_THRESHOLD = 200_000

/** Подсветка совпадений поиска в EditText: текущее — ярче, остальные — приглушённее. */
private val CURRENT_HIT_COLOR = AppColors.SearchHitCurrent.toArgb()
private val OTHER_HIT_COLOR = AppColors.SearchHitOther.toArgb()

/**
 * Создание ([workFile] == null) или редактирование произведения. Два режима:
 * данные книги (метаданные) и полноэкранный редактор текста с панелью
 * форматирования внизу — переключаются раздельно.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkEditScreen(
    authorLink: String,
    workFile: String?,
    onBack: () -> Unit,
    viewModel: WorkEditViewModel =
        koinViewModel(key = "workedit:$authorLink:$workFile") {
            parametersOf(authorLink, workFile)
        },
) {
    val state by viewModel.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(Unit) { viewModel.load() }
    viewModel.CollectEditEffects(
        onDone = onBack,
        deletedMessage = stringResource(R.string.workedit_deleted_message),
    )

    // Выбор файла с телом произведения → чтение байтов в память.
    val fileReadErrorMessage = stringResource(R.string.workedit_file_read_error)
    val filePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                val bytes =
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    }.getOrNull()
                if (bytes != null && bytes.isNotEmpty()) {
                    val name =
                        runCatching {
                            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                                val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                if (i >= 0 && c.moveToFirst()) c.getString(i) else null
                            }
                        }.getOrNull() ?: uri.lastPathSegment ?: "upload.txt"
                    viewModel.attachFile(name, bytes, context.contentResolver.getType(uri))
                } else {
                    Toast.makeText(context, fileReadErrorMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }

    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.workedit_delete_confirm_title),
            text = stringResource(R.string.workedit_delete_confirm_text, state.title),
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }

    // Режим полноэкранного редактора текста. Большой текст правится в нативном
    // EditText (Compose-поле тормозит на объёмных документах); тело синхронизируется
    // во ViewModel не на каждый символ, а при выходе/сохранении — см. syncText().
    var editingText by rememberSaveable { mutableStateOf(false) }
    var editor by remember { mutableStateOf<EditText?>(null) }
    var scrollHost by remember { mutableStateOf<NestedScrollView?>(null) }
    var previewMode by rememberSaveable { mutableStateOf(false) }
    var pendingOffset by remember { mutableStateOf(0) }
    val listState = rememberLazyListState()
    // Скроллбар для режима правки — как в превью/читалке, но источник прогресса не
    // LazyListState, а сам NestedScrollView (scrollY / величина скролла).
    var editScrollProgress by remember { mutableStateOf(0f) }
    var editCanScroll by remember { mutableStateOf(false) }

    fun editMaxScroll(): Int {
        val host = scrollHost ?: return 0
        val child = host.getChildAt(0) ?: return 0
        return (child.height - host.height).coerceAtLeast(0)
    }

    fun updateEditScrollProgress() {
        val host = scrollHost ?: return
        val maxScroll = editMaxScroll()
        editCanScroll = maxScroll > 0
        editScrollProgress =
            if (maxScroll > 0) (host.scrollY.toFloat() / maxScroll).coerceIn(0f, 1f) else 0f
    }

    fun scrollEditToFraction(f: Float) {
        val host = scrollHost ?: return
        val maxScroll = editMaxScroll()
        host.scrollTo(0, (f.coerceIn(0f, 1f) * maxScroll).toInt())
        updateEditScrollProgress()
    }

    var showEditPercentDialog by remember { mutableStateOf(false) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var currentHitIndex by rememberSaveable { mutableStateOf(-1) }
    val searchResult = remember(state.text, searchQuery) { findDraftHits(state.text, searchQuery) }
    LaunchedEffect(searchResult.hits) {
        currentHitIndex = if (searchResult.hits.isEmpty()) -1 else 0
    }
    val editScope = rememberCoroutineScope()

    fun jumpToHit(index: Int) {
        val hit = searchResult.hits.getOrNull(index) ?: return
        if (previewMode) {
            val blockIndex = draftBlockIndexForOffset(state.draftBlocks, hit.first)
            if (blockIndex >= 0) editScope.launch { listState.animateScrollToItem(blockIndex) }
        } else {
            val et = editor ?: return
            val layout = et.layout ?: return
            val line = layout.getLineForOffset(hit.first)
            scrollHost?.scrollTo(0, layout.getLineTop(line))
            et.setSelection(hit.first, hit.last + 1)
        }
    }
    // Подсветка всех совпадений прямо в тексте — текущее ярче остальных; setSelection
    // (в jumpToHit) без фокуса на EditText не виден, а фон нужен независимо от фокуса.
    LaunchedEffect(editor, searchResult.hits, currentHitIndex) {
        val et = editor ?: return@LaunchedEffect
        val editable = et.text
        editable
            .getSpans(0, editable.length, BackgroundColorSpan::class.java)
            .forEach { editable.removeSpan(it) }
        searchResult.hits.forEachIndexed { index, hit ->
            val color = if (index == currentHitIndex) CURRENT_HIT_COLOR else OTHER_HIT_COLOR
            val end = (hit.last + 1).coerceAtMost(editable.length)
            if (hit.first < end) {
                editable.setSpan(
                    BackgroundColorSpan(color),
                    hit.first,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
        }
    }
    val syncText = { editor?.let { viewModel.setText(it.text?.toString().orEmpty()) } }

    // Панель форматирования читает/пишет выделение прямо в EditText по требованию.
    val formatTarget =
        remember {
            object : FormatTarget {
                override fun current(): TextFieldValue {
                    val et = editor ?: return TextFieldValue("")
                    val text = et.text?.toString().orEmpty()
                    val start = et.selectionStart.coerceIn(0, text.length)
                    val end = et.selectionEnd.coerceIn(0, text.length)
                    return TextFieldValue(text, TextRange(minOf(start, end), maxOf(start, end)))
                }

                override fun apply(value: TextFieldValue) {
                    val et = editor ?: return
                    val old = et.text?.toString().orEmpty()
                    val edit = computeReplaceEdit(old, value.text)
                    et.text.replace(edit.start, edit.endOld, edit.replacement)
                    et.setSelection(value.selection.start.coerceIn(0, value.text.length))
                }
            }
        }

    if (editingText) {
        BackHandler {
            syncText()
            editingText = false
        }
        var precomputedText by remember { mutableStateOf<PrecomputedTextCompat?>(null) }
        var precomputing by remember { mutableStateOf(state.text.length > PRECOMPUTE_THRESHOLD) }
        LaunchedEffect(state.text) {
            if (state.text.length <= PRECOMPUTE_THRESHOLD) {
                precomputedText = null
                precomputing = false
                return@LaunchedEffect
            }
            precomputing = true
            val params = TextViewCompat.getTextMetricsParams(EditText(context))
            precomputedText =
                withContext(Dispatchers.Default) {
                    PrecomputedTextCompat.create(state.text, params)
                }
            precomputing = false
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.workedit_text_title)) },
                    navigationIcon = {
                        IconButton(onClick = {
                            syncText()
                            editingText = false
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.workedit_back_to_data),
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSearch = !showSearch }) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = stringResource(R.string.workedit_search_in_text),
                            )
                        }
                        TextButton(onClick = {
                            syncText()
                            pendingOffset =
                                if (previewMode) {
                                    state.draftBlocks
                                        .getOrNull(listState.firstVisibleItemIndex)
                                        ?.rawRange
                                        ?.first
                                        ?: 0
                                } else {
                                    editor?.layout?.let { layout ->
                                        layout.getLineStart(
                                            layout.getLineForVertical(
                                                scrollHost?.scrollY ?: 0,
                                            ),
                                        )
                                    } ?: 0
                                }
                            previewMode = !previewMode
                        }) {
                            Text(
                                if (previewMode) {
                                    stringResource(R.string.workedit_mode_edit)
                                } else {
                                    stringResource(R.string.workedit_mode_preview)
                                },
                            )
                        }
                        IconButton(
                            onClick = {
                                syncText()
                                viewModel.save()
                            },
                            enabled = !state.saving,
                        ) {
                            if (state.saving) {
                                CircularProgressIndicator(Modifier.size(20.dp))
                            } else {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = stringResource(R.string.workedit_save),
                                )
                            }
                        }
                    },
                )
            },
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding(),
            ) {
                if (showSearch) {
                    EditorSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        hitCount = searchResult.hits.size,
                        currentHitIndex = currentHitIndex,
                        onPrevHit = {
                            if (searchResult.hits.isNotEmpty()) {
                                currentHitIndex =
                                    (currentHitIndex - 1 + searchResult.hits.size) % searchResult.hits.size
                                jumpToHit(currentHitIndex)
                            }
                        },
                        onNextHit = {
                            if (searchResult.hits.isNotEmpty()) {
                                currentHitIndex = (currentHitIndex + 1) % searchResult.hits.size
                                jumpToHit(currentHitIndex)
                            }
                        },
                        onClose = {
                            showSearch = false
                            searchQuery = ""
                        },
                    )
                }
                // Непрозрачный фон на EditText/NestedScrollView — иначе, если Android-View
                // перерисуется не в тот момент (например, после программного scrollTo), под ней
                // на кадр проступают соседние Compose-элементы (панель поиска/тулбар).
                val editorBackground = MaterialTheme.colorScheme.background.toArgb()
                // EditText с 895k символов пересобирать при каждом переключении превью↔правка
                // дорого (полная перезаливка Editable + спаны поиска — фриз на возврате из
                // превью). Поэтому AndroidView не убирается из композиции по previewMode —
                // он монтируется один раз, как только готов precomputedText, и дальше просто
                // скрывается (height(0.dp)), а не пересоздаётся.
                val editorActive = !previewMode
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    if (previewMode && state.chunkingDraft) {
                        LoadingBox(Modifier.fillMaxSize())
                    } else if (previewMode) {
                        EditorPreviewPane(
                            draftBlocks = state.draftBlocks,
                            pendingOffset = pendingOffset,
                            listState = listState,
                        )
                    } else if (precomputing) {
                        LoadingBox(Modifier.fillMaxSize())
                    }
                    // AndroidView остаётся смонтированным и при previewMode (см. комментарий выше) —
                    // NativeEditorPane сам скрывается (height(0.dp)), но не пересоздаётся.
                    if (!precomputing) {
                        NativeEditorPane(
                            initialText = state.text,
                            editorBackground = editorBackground,
                            precomputedText = precomputedText,
                            precomputing = precomputing,
                            pendingOffset = pendingOffset,
                            editorActive = editorActive,
                            editCanScroll = editCanScroll,
                            editScrollProgress = editScrollProgress,
                            onEditorCreated = { editor = it },
                            onScrollHostCreated = { scrollHost = it },
                            onReleased = { released ->
                                if (scrollHost === released) {
                                    scrollHost = null
                                    editor = null
                                }
                            },
                            onScrollChanged = { updateEditScrollProgress() },
                            onTapPercent = { showEditPercentDialog = true },
                            onScrollToFraction = { f -> scrollEditToFraction(f) },
                        )
                    }
                }
                if (editorActive && showEditPercentDialog) {
                    TextPositionDialog(
                        currentFraction = editScrollProgress,
                        onDismiss = { showEditPercentDialog = false },
                        onGo = { f ->
                            showEditPercentDialog = false
                            scrollEditToFraction(f)
                        },
                    )
                }
                HorizontalDivider()
                // панель форматирования — только в режиме редактирования, не в превью
                if (!previewMode) {
                    Surface(tonalElevation = 3.dp) {
                        FormatToolbar(target = formatTarget)
                    }
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isEdit) {
                            stringResource(R.string.workedit_title_edit)
                        } else {
                            stringResource(R.string.workedit_title_new)
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.workedit_back),
                        )
                    }
                },
                actions = {
                    if (state.loaded && state.isEdit) {
                        IconButton(onClick = { confirmDelete = true }, enabled = !state.saving) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.workedit_delete),
                            )
                        }
                    }
                    if (state.loaded) {
                        IconButton(onClick = { viewModel.save() }, enabled = !state.saving) {
                            if (state.saving) {
                                CircularProgressIndicator(Modifier.size(20.dp))
                            } else {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = stringResource(R.string.workedit_save),
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading -> LoadingBox(Modifier.padding(padding))
            state.error != null ->
                ErrorBox(
                    state.error!!,
                    viewModel::load,
                    Modifier.padding(padding),
                )

            else ->
                Column(
                    Modifier
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    OutlinedTextField(
                        value = state.fileName,
                        onValueChange = viewModel::setFileName,
                        label = { Text(stringResource(R.string.workedit_filename_label)) },
                        singleLine = true,
                        enabled = !state.isEdit,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                    )
                    OutlinedTextField(
                        value = state.title,
                        onValueChange = viewModel::setTitle,
                        label = { Text(stringResource(R.string.workedit_title_label)) },
                        singleLine = true,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                    )
                    OptionDropdown(
                        stringResource(R.string.workedit_form_label),
                        state.types,
                        state.type,
                        viewModel::setType,
                    )
                    OptionDropdown(
                        stringResource(R.string.workedit_genre_label),
                        state.janrs,
                        state.janr,
                        viewModel::setJanr,
                    )
                    if (state.groups.size > 1) {
                        OptionDropdown(
                            stringResource(R.string.workedit_group_label),
                            state.groups,
                            state.group,
                            viewModel::setGroup,
                        )
                    }
                    OutlinedTextField(
                        value = state.annotation,
                        onValueChange = viewModel::setAnnotation,
                        label = { Text(stringResource(R.string.workedit_annotation_label)) },
                        minLines = 3,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                    )
                    if (!state.isEdit) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp),
                        ) {
                            AssistChip(
                                onClick = { filePicker.launch("*/*") },
                                label = {
                                    Text(
                                        state.attachedFileName
                                            ?: stringResource(R.string.workedit_upload_file),
                                    )
                                },
                            )
                            if (state.attachedFileName != null) {
                                IconButton(onClick = { viewModel.detachFile() }) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = stringResource(R.string.workedit_detach_file),
                                    )
                                }
                            }
                        }
                    }
                    if (state.attachedFileName == null) {
                        // текст редактируется отдельно — на полном экране с форматированием
                        OutlinedButton(
                            onClick = { editingText = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null)
                            Text(
                                "  " +
                                    if (state.text.isBlank()) {
                                        stringResource(R.string.workedit_write_text)
                                    } else {
                                        stringResource(R.string.workedit_edit_text)
                                    },
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                    }
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionDropdown(
    label: String,
    options: List<SelectOption>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = options.firstOrNull { it.value == selected }?.label ?: "",
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier =
                Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        expanded = false
                        onSelect(option.value)
                    },
                )
            }
        }
    }
}
