package ru.samlib.client.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/** Маленькая красная метка «new»/«обновлено» рядом с названием. */
@Composable
fun UpdatedBadge(
    text: String = "new",
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onError,
        style = MaterialTheme.typography.labelSmall,
        modifier =
            modifier
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.error)
                .padding(horizontal = 5.dp, vertical = 1.dp),
    )
}
