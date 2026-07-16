package ru.samlib.client.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ru.samlib.client.ui.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onAction: (LoginAction) -> Unit,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val state by viewModel.collectAsState()
    LaunchedEffect(Unit) { viewModel.observe() }
    // После входа открываем личную страницу автора (если известна), иначе назад.
    viewModel.collectSideEffect { effect ->
        when (effect) {
            is LoginSideEffect.Success ->
                onAction(effect.authorLink.takeIf { it.isNotBlank() }?.let(LoginAction::OpenAuthor) ?: LoginAction.Back)
        }
    }

    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.login_title)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(LoginAction.Back) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.login_back),
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.loggedIn) {
                Text(
                    stringResource(R.string.login_logged_in),
                    style = MaterialTheme.typography.titleMedium,
                )
                Button(onClick = { viewModel.openMyPage() }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.login_my_page))
                }
                OutlinedButton(
                    onClick = { viewModel.logout() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.login_logout))
                }
            } else {
                OutlinedTextField(
                    value = login,
                    onValueChange = { login = it },
                    label = { Text(stringResource(R.string.login_login)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.login_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                state.error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Button(
                    onClick = { viewModel.login(login, password) },
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.loading) {
                        CircularProgressIndicator(
                            Modifier
                                .padding(end = 8.dp)
                                .size(18.dp),
                        )
                    }
                    Text(stringResource(R.string.login_submit))
                }
            }
        }
    }
}
