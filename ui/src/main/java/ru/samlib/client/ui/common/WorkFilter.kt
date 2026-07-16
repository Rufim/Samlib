package ru.samlib.client.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.samlib.client.domain.model.Gender
import ru.samlib.client.domain.model.Genre
import ru.samlib.client.domain.model.WorkFilter
import ru.samlib.client.ui.R

/**
 * Полноэкранная страница фильтра (диалог во весь экран, результат уходит в [onApply]).
 * Выбранные жанры — чипы с крупным крестиком; добавление — диалог с ещё не выбранными.
 * [showQuery] = показывать поле текста (для истории есть отдельный поиск).
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FilterScreen(
    initial: WorkFilter,
    showQuery: Boolean = true,
    onApply: (WorkFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf(initial.query) }
    var genres by remember { mutableStateOf(initial.genres) }
    var genders by remember { mutableStateOf(initial.genders) }
    var exclude by remember { mutableStateOf(initial.exclude) }
    var showAddGenres by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.filter_title)) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.common_close),
                            )
                        }
                    },
                    actions = {
                        TextButton(onClick = { onApply(WorkFilter()) }) { Text(stringResource(R.string.filter_reset)) }
                        IconButton(onClick = {
                            onApply(
                                WorkFilter(
                                    query,
                                    genres,
                                    genders,
                                    exclude,
                                ),
                            )
                        }) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = stringResource(R.string.filter_apply),
                            )
                        }
                    },
                )
            },
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (showQuery) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text(stringResource(R.string.filter_search_label)) },
                        singleLine = true,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                    )
                }
                Text(
                    stringResource(R.string.filter_genres),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (genres.isEmpty()) {
                    Text(
                        stringResource(R.string.filter_genres_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
                FlowRow(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    genres.sortedBy { it.title }.forEach { genre ->
                        InputChip(
                            selected = true,
                            onClick = { genres = genres - genre },
                            label = {
                                Text(
                                    genre.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription =
                                        stringResource(
                                            R.string.filter_remove_genre,
                                            genre.title,
                                        ),
                                    modifier = Modifier.size(24.dp),
                                )
                            },
                            modifier = Modifier.height(40.dp),
                        )
                    }
                    AssistChip(
                        onClick = { showAddGenres = true },
                        label = {
                            Text(
                                stringResource(R.string.filter_add),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        modifier = Modifier.height(40.dp),
                    )
                }
                Text(
                    stringResource(R.string.filter_author_gender),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val genderTitles =
                        listOf(
                            Gender.MALE to stringResource(R.string.filter_gender_male),
                            Gender.FEMALE to stringResource(R.string.filter_gender_female),
                            Gender.UNDEFINED to stringResource(R.string.filter_gender_unknown),
                        )
                    genderTitles.forEach { (g, title) ->
                        val selected = g in genders
                        FilterChip(
                            selected = selected,
                            onClick = { genders = if (selected) genders - g else genders + g },
                            label = { Text(title) },
                        )
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.filter_exclude))
                    Switch(checked = exclude, onCheckedChange = { exclude = it })
                }
            }
        }
    }

    if (showAddGenres) {
        val available = Genre.entries.filter { it != Genre.EMPTY && it !in genres }
        AlertDialog(
            onDismissRequest = { showAddGenres = false },
            confirmButton = {
                TextButton(onClick = {
                    showAddGenres = false
                }) { Text(stringResource(R.string.common_done)) }
            },
            title = { Text(stringResource(R.string.filter_add_genres)) },
            text = {
                if (available.isEmpty()) {
                    Text(stringResource(R.string.filter_all_genres_added))
                } else {
                    LazyColumn(Modifier.heightIn(max = 420.dp)) {
                        items(available, key = { it.name }) { genre ->
                            Text(
                                genre.title,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { genres = genres + genre }
                                        .padding(vertical = 12.dp, horizontal = 4.dp),
                            )
                            HorizontalDivider()
                        }
                    }
                }
            },
        )
    }
}
