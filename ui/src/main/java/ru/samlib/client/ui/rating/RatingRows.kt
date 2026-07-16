package ru.samlib.client.ui.rating

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.samlib.client.domain.model.Author
import ru.samlib.client.domain.model.Discussion
import ru.samlib.client.domain.model.Work
import ru.samlib.client.ui.R
import ru.samlib.client.ui.theme.SamlibTheme
import java.math.BigDecimal

/** Разделяет число разрядами: 423394 → «423 394». */
private fun groupThousands(n: Int): String = "%,d".format(n).replace(',', ' ')

/**
 * Строка «Топ авторов». По требованию: сначала метрики (просмотры и рейтинг),
 * затем имя автора, затем описание.
 */
@Composable
fun AuthorRow(
    author: Author,
    onAuthor: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable { onAuthor(author.link) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        val metrics =
            listOfNotNull(
                author.views?.let { "👁 ${groupThousands(it)}" },
                author.rate?.let { rate ->
                    "★ $rate" + (author.votes?.let { " ($it)" }.orEmpty())
                },
                author.workCount?.let { stringResource(R.string.rating_works_count, it) },
            )
        if (metrics.isNotEmpty()) {
            Text(
                metrics.joinToString("   •   "),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            author.fullName ?: author.link,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 2.dp),
        )
        author.annotation?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/** Строка произведения (Обзоры / Избранное). */
@Composable
fun WorkRow(
    work: Work,
    onOpenWork: (String) -> Unit,
    onAuthor: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable { onOpenWork(work.link) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            work.title.orEmpty(),
            style = MaterialTheme.typography.titleMedium,
        )
        val metrics =
            listOfNotNull(
                work.rate?.let { r -> "★ $r" + (work.votes?.let { " ($it)" }.orEmpty()) },
                work.expertRate?.let { e ->
                    stringResource(
                        R.string.rating_expert_prefix,
                        e,
                    ) + (work.expertVotes?.let { " ($it)" }.orEmpty())
                },
                work.size?.let { "${it}k" },
            )
        if (metrics.isNotEmpty()) {
            Text(
                metrics.joinToString("   •   "),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        work.customGenres?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        work.authorShortName?.let { name ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                SuggestionChip(
                    onClick = { work.authorLink?.let(onAuthor) },
                    enabled = work.authorLink != null,
                    label = { Text(name) },
                )
            }
        }
    }
}

/** Строка обсуждения. Клик по строке ведёт на экран комментариев. */
@Composable
fun DiscussionRow(
    discussion: Discussion,
    onOpenComments: (String) -> Unit,
    onAuthor: (String) -> Unit,
) {
    val work = discussion.work
    val commentsTarget = discussion.commentsLink ?: work.link
    Column(
        Modifier
            .fillMaxWidth()
            .clickable { onOpenComments(commentsTarget) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            work.title.orEmpty(),
            style = MaterialTheme.typography.titleMedium,
        )
        val counts = listOfNotNull(discussion.count, discussion.countOfDay).joinToString("/")
        val metrics =
            listOfNotNull(
                counts.takeIf { it.isNotBlank() }?.let { "💬 $it" },
                discussion.lastOne?.let { stringResource(R.string.rating_last_prefix, it) },
            )
        if (metrics.isNotEmpty()) {
            Text(
                metrics.joinToString("   •   "),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp),
        ) {
            discussion.author.fullName?.let { name ->
                SuggestionChip(
                    onClick = { discussion.author.link.let(onAuthor) },
                    label = { Text(name) },
                )
                Spacer(Modifier.width(8.dp))
            }
            work.customGenres?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthorRowPreview() {
    SamlibTheme {
        AuthorRow(
            author =
                Author(
                    link = "/i/ivanov_i/",
                    fullName = "Иванов Иван",
                    views = 12345,
                    rate = BigDecimal("4.8"),
                    votes = 120,
                    workCount = 7,
                    annotation = "Короткая аннотация автора для превью карточки.",
                ),
            onAuthor = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkRowRatingPreview() {
    SamlibTheme {
        WorkRow(
            work =
                Work(
                    link = "/i/ivanov_i/rasskaz.shtml",
                    title = "Название произведения",
                    rate = BigDecimal("4.5"),
                    votes = 30,
                    size = 120,
                    customGenres = "Фантастика",
                    authorShortName = "Иванов И.",
                    authorLink = "/i/ivanov_i/",
                ),
            onOpenWork = {},
            onAuthor = {},
        )
    }
}
