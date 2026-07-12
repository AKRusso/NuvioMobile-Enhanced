package com.nuvio.app.features.cloudstream

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Keeps the exact provider routes returned by CloudStream search endpoints.
 *
 * A normal Nuvio poster only has external metadata, while a CloudStream search card
 * already contains the provider-specific URL required by MainAPI.load. Remembering
 * that URL makes both entry paths resolve through the same CloudStream contract.
 */
internal object CloudStreamSearchRouteIndex {
    private data class Entry(
        val providerId: String,
        val query: String,
        val item: CloudStreamSearchItem,
    )

    private val entries = MutableStateFlow<List<Entry>>(emptyList())

    fun remember(
        providerId: String,
        query: String,
        items: List<CloudStreamSearchItem>,
    ) {
        if (items.isEmpty()) return
        val additions = items.map { item ->
            Entry(providerId = providerId, query = query.normalizedRouteTitle(), item = item)
        }
        entries.update { current ->
            (additions + current)
                .distinctBy { entry -> entry.providerId to entry.item.data }
                .take(MAX_ENTRIES)
        }
    }

    fun find(
        providerId: String,
        titles: List<String>,
        type: String,
        year: Int?,
    ): List<CloudStreamSearchItem> {
        val normalizedTitles = titles.map { title -> title.normalizedRouteTitle() }.filter(String::isNotBlank).toSet()
        return entries.value.asSequence()
            .filter { entry -> entry.providerId == providerId }
            .filter { entry -> year == null || entry.item.year == null || entry.item.year == year }
            .filter { entry ->
                val itemTitle = entry.item.name.normalizedRouteTitle()
                itemTitle in normalizedTitles || normalizedTitles.any { title ->
                    itemTitle.startsWith(title) || title.startsWith(itemTitle)
                }
            }
            .map(Entry::item)
            .distinctBy(CloudStreamSearchItem::data)
            .take(MAX_MATCHES)
            .toList()
    }

    private fun String.normalizedRouteTitle(): String = lowercase()
        .map { char -> if (char.isLetterOrDigit()) char else ' ' }
        .joinToString("")
        .trim()
        .split(Regex("\\s+"))
        .filter(String::isNotBlank)
        .joinToString(" ")

    private const val MAX_ENTRIES = 1_000
    private const val MAX_MATCHES = 5
}
