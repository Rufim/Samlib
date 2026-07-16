package ru.samlib.client.ui.author

import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ru.samlib.client.ui.R
import ru.samlib.client.ui.common.ErrorBox
import ru.samlib.client.ui.common.FormatToolbar
import ru.samlib.client.ui.common.LoadingBox

/** Редактор текста «о себе» (Сведения об авторе) для своей страницы. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutEditScreen(
    authorLink: String,
    onBack: () -> Unit,
    viewModel: AboutEditViewModel = koinViewModel { parametersOf(authorLink) },
) {
    val state by viewModel.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(Unit) { viewModel.load() }
    viewModel.collectSideEffect { effect ->
        when (effect) {
            is AboutEditSideEffect.Saved -> {
                Toast
                    .makeText(
                        context,
                        context.getString(R.string.aboutedit_saved),
                        Toast.LENGTH_SHORT,
                    ).show()
                onBack()
            }

            is AboutEditSideEffect.Error ->
                Toast.makeText(context, effect.message, Toast.LENGTH_LONG).show()

            is AboutEditSideEffect.AvatarResult ->
                Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
        }
    }
    // Выбор изображения аватара → чтение байтов → загрузка.
    val avatarPicker =
        androidx.activity.compose.rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent(),
        ) { uri ->
            if (uri != null) {
                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                val bytes =
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    }.getOrNull()
                if (bytes != null && bytes.isNotEmpty()) {
                    viewModel.uploadAvatar(bytes, mime)
                } else {
                    Toast
                        .makeText(
                            context,
                            context.getString(R.string.aboutedit_file_read_error),
                            Toast.LENGTH_SHORT,
                        ).show()
                }
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.aboutedit_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.aboutedit_back),
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
                                    contentDescription = stringResource(R.string.aboutedit_save),
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
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                ) {
                    OutlinedButton(
                        onClick = { avatarPicker.launch("image/*") },
                        enabled = !state.saving,
                        modifier = Modifier.padding(bottom = 12.dp),
                    ) { Text(stringResource(R.string.aboutedit_change_avatar)) }
                    var textField by remember(state.loaded) {
                        mutableStateOf(TextFieldValue(state.text))
                    }
                    FormatToolbar(
                        value = textField,
                        onChange = {
                            textField = it
                            viewModel.setText(it.text)
                        },
                    )
                    OutlinedTextField(
                        value = textField,
                        onValueChange = {
                            textField = it
                            viewModel.setText(it.text)
                        },
                        label = { Text(stringResource(R.string.aboutedit_text_label)) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
        }
    }
}
