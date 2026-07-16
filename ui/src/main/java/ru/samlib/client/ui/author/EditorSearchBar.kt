package ru.samlib.client.ui.author

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.samlib.client.ui.R

/** Строка поиска по тексту черновика: запрос, счётчик совпадений, переход по ним. */
@Composable
fun EditorSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    hitCount: Int,
    currentHitIndex: Int,
    onPrevHit: () -> Unit,
    onNextHit: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            placeholder = { Text(stringResource(R.string.workedit_search_placeholder)) },
            modifier = Modifier.weight(1f),
        )
        Text(
            if (hitCount == 0) "0/0" else "${currentHitIndex + 1}/$hitCount",
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        TextButton(onClick = onPrevHit) { Text("▲") }
        TextButton(onClick = onNextHit) { Text("▼") }
        IconButton(onClick = onClose) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.workedit_close_search),
            )
        }
    }
    HorizontalDivider()
}
