package ru.samlib.client.ui.author

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import org.orbitmvi.orbit.compose.collectAsState
import ru.samlib.client.ui.R
import ru.samlib.client.ui.common.CollectEditEffects
import ru.samlib.client.ui.common.ErrorBox
import ru.samlib.client.ui.common.LoadingBox

/** Редактор групп (разделов) авторской страницы (/cgi-bin/groupedit). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupEditScreen(
    authorLink: String,
    onBack: () -> Unit,
    viewModel: GroupEditViewModel = koinViewModel { parametersOf(authorLink) },
) {
    val state by viewModel.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }
    viewModel.CollectEditEffects(onDone = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.groupedit_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.groupedit_back),
                        )
                    }
                },
                actions = {
                    if (state.loaded) {
                        IconButton(onClick = { viewModel.save() }, enabled = !state.saving) {
                            if (state.saving) {
                                CircularProgressIndicator(Modifier.size(20.dp))
                            } else {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = stringResource(R.string.groupedit_save),
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
                    state.groups.forEachIndexed { index, group ->
                        ElevatedCard(
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        stringResource(R.string.groupedit_group_number, index + 1),
                                        modifier = Modifier.weight(1f),
                                    )
                                    IconButton(onClick = { viewModel.removeGroup(index) }) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = stringResource(R.string.groupedit_delete_group),
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = group.title,
                                    onValueChange = { viewModel.setTitle(index, it) },
                                    label = { Text(stringResource(R.string.groupedit_name_label)) },
                                    singleLine = true,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp),
                                )
                                OutlinedTextField(
                                    value = group.annotation,
                                    onValueChange = { viewModel.setAnnotation(index, it) },
                                    label = { Text(stringResource(R.string.groupedit_annotation_label)) },
                                    minLines = 2,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                    if (state.canAdd) {
                        OutlinedButton(
                            onClick = { viewModel.addGroup() },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Text(
                                stringResource(R.string.groupedit_add_group),
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
        }
    }
}
