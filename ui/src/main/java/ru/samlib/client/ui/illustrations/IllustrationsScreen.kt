package ru.samlib.client.ui.illustrations

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import org.orbitmvi.orbit.compose.collectAsState
import ru.samlib.client.domain.model.Image
import ru.samlib.client.ui.R
import ru.samlib.client.ui.common.ErrorBox
import ru.samlib.client.ui.common.LoadingBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IllustrationsScreen(
    link: String,
    onBack: () -> Unit,
    viewModel: IllustrationsViewModel = koinViewModel { parametersOf(link) },
) {
    val state by viewModel.collectAsState()
    LaunchedEffect(link) { viewModel.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.illustrations_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
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

            state.images.isEmpty() ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) { Text(stringResource(R.string.illustrations_empty)) }

            else ->
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding),
                ) {
                    items(state.images, key = { it.number }) { image -> IllustrationItem(image) }
                }
        }
    }
}

@Composable
private fun IllustrationItem(image: Image) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        AsyncImage(
            model = image.url,
            contentDescription = image.title,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth(),
        )
        image.title?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        image.annotation?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val meta =
            listOfNotNull(
                image.width?.let { w -> image.height?.let { h -> "$w×$h" } },
                image.sizeKb?.let { "${it}k" },
            )
        if (meta.isNotEmpty()) {
            Text(
                meta.joinToString(" • "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
