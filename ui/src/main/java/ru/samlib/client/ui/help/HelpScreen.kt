package ru.samlib.client.ui.help

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.samlib.client.domain.samlib.SamlibUrls
import ru.samlib.client.ui.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.help_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.help_back),
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.help_app_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                stringResource(R.string.help_app_description),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                stringResource(R.string.help_sections_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(R.string.help_sections_text),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                stringResource(R.string.help_reading_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(R.string.help_reading_text),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = { uriHandler.openUri("${SamlibUrls.BASE}/") }) {
                Text(stringResource(R.string.help_open_samlib))
            }
        }
    }
}
