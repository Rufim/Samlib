package ru.samlib.client.ui.author

import ru.samlib.client.domain.repository.DraftBlock
import ru.samlib.client.ui.work.SearchDefaults

/** Минимальная правка, переводящая [start]..[endOld) старого текста в [replacement]. */
data class ReplaceEdit(
    val start: Int,
    val endOld: Int,
    val replacement: String,
)

/**
 * Вычисляет минимальный диапазон замены между [old] и [new] (общий префикс/суффикс не входят
 * в правку). Используется вместо полной замены текста в `EditText`, чтобы форматирование
 * (жирный/курсив/…) на многомегабайтном черновике не пересобирало DynamicLayout целиком.
 */
fun computeReplaceEdit(
    old: String,
    new: String,
): ReplaceEdit {
    val maxCommon = minOf(old.length, new.length)
    var prefix = 0
    while (prefix < maxCommon && old[prefix] == new[prefix]) prefix++
    var suffix = 0
    while (suffix < maxCommon - prefix &&
        old[old.length - 1 - suffix] == new[new.length - 1 - suffix]
    ) {
        suffix++
    }
    val start = prefix
    val endOld = old.length - suffix
    val replacement = new.substring(prefix, new.length - suffix)
    return ReplaceEdit(start, endOld, replacement)
}

/** Индекс блока в [draftBlocks], чей [DraftBlock.rawRange] содержит [offset] (или последний, если offset за концом текста). */
fun draftBlockIndexForOffset(
    draftBlocks: List<DraftBlock>,
    offset: Int,
): Int {
    if (draftBlocks.isEmpty()) return -1
    val idx = draftBlocks.indexOfFirst { offset <= it.rawRange.last }
    return if (idx >= 0) idx else draftBlocks.lastIndex
}

/** Результат поиска: диапазоны символов в исходном тексте, [truncated] — если хитов больше MAX_SEARCH_HITS. */
data class DraftSearchResult(
    val hits: List<IntRange>,
    val truncated: Boolean,
) {
    companion object {
        val EMPTY = DraftSearchResult(emptyList(), false)
    }
}

/** Ищет [rawQuery] в [text] без учёта регистра; хиты — диапазоны символов в исходном тексте. */
fun findDraftHits(
    text: String,
    rawQuery: String,
): DraftSearchResult {
    val query = rawQuery.trim()
    if (query.length < MIN_SEARCH_QUERY) return DraftSearchResult.EMPTY
    val needle = query.lowercase()
    val haystack = text.lowercase()
    val hits = mutableListOf<IntRange>()
    var from = 0
    var truncated = false
    while (true) {
        val idx = haystack.indexOf(needle, from)
        if (idx < 0) break
        hits.add(idx until idx + query.length)
        from = idx + query.length
        if (hits.size >= MAX_SEARCH_HITS) {
            truncated = true
            break
        }
    }
    return DraftSearchResult(hits, truncated)
}

// Единые пороги с поиском по книге в читалке — см. SearchDefaults.
private const val MIN_SEARCH_QUERY = SearchDefaults.MIN_QUERY
private const val MAX_SEARCH_HITS = SearchDefaults.MAX_HITS
