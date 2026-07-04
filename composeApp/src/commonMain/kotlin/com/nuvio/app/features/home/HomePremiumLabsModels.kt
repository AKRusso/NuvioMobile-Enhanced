package com.nuvio.app.features.home

import com.nuvio.app.features.library.LibraryItem
import com.nuvio.app.features.library.toMetaPreview
import com.nuvio.app.features.watchprogress.ContinueWatchingItem

internal data class HomeSmartShelf(
    val key: String,
    val title: String,
    val subtitle: String,
    val signal: String,
    val items: List<MetaPreview>,
)

internal fun buildHomeSmartShelves(
    continueWatchingItems: List<ContinueWatchingItem>,
    libraryItems: List<LibraryItem>,
    catalogSections: List<HomeCatalogSection>,
): List<HomeSmartShelf> {
    val catalogItems = catalogSections
        .flatMap(HomeCatalogSection::items)
        .distinctBy(MetaPreview::stableKey)
    val libraryPreviews = libraryItems
        .map(LibraryItem::toMetaPreview)
        .distinctBy(MetaPreview::stableKey)

    return buildList {
        continueWatchingItems
            .filter { item -> item.progressFraction in 0.08f..0.92f || item.isNextUp }
            .map(ContinueWatchingItem::toSmartShelfPreview)
            .distinctBy(MetaPreview::stableKey)
            .take(HomeSmartShelfItemLimit)
            .takeIf(List<MetaPreview>::isNotEmpty)
            ?.let { items ->
                add(
                    HomeSmartShelf(
                        key = "resume-intent",
                        title = "Resume Intent",
                        subtitle = "Progress-aware picks that are already warm.",
                        signal = "Progress",
                        items = items,
                    ),
                )
            }

        libraryPreviews
            .sortedWith(
                compareByDescending<MetaPreview> { item -> item.imdbRating.smartShelfRatingOrNull() ?: -1.0 }
                    .thenBy { item -> item.name.lowercase() },
            )
            .take(HomeSmartShelfItemLimit)
            .takeIf(List<MetaPreview>::isNotEmpty)
            ?.let { items ->
                add(
                    HomeSmartShelf(
                        key = "library-high-signal",
                        title = "Library High Signal",
                        subtitle = "The strongest saved titles in this profile.",
                        signal = "Library",
                        items = items,
                    ),
                )
            }

        catalogItems
            .sortedWith(
                compareByDescending<MetaPreview> { item -> item.imdbRating.smartShelfRatingOrNull() ?: -1.0 }
                    .thenByDescending { item -> item.popularity ?: -1.0 }
                    .thenByDescending { item -> item.voteCount ?: -1 },
            )
            .take(HomeSmartShelfItemLimit)
            .takeIf(List<MetaPreview>::isNotEmpty)
            ?.let { items ->
                add(
                    HomeSmartShelf(
                        key = "critic-pulse",
                        title = "Critic Pulse",
                        subtitle = "High-rated catalog discoveries with visible momentum.",
                        signal = "Quality",
                        items = items,
                    ),
                )
            }

        val genreFocus = (libraryPreviews + catalogItems)
            .flatMap { item -> item.genres.take(2) }
            .map(String::trim)
            .filter(String::isNotBlank)
            .groupingBy { it.lowercase() }
            .eachCount()
            .maxByOrNull { (_, count) -> count }
            ?.key

        if (genreFocus != null) {
            val focusedItems = (libraryPreviews + catalogItems)
                .distinctBy(MetaPreview::stableKey)
                .filter { item -> item.genres.any { genre -> genre.equals(genreFocus, ignoreCase = true) } }
                .take(HomeSmartShelfItemLimit)
            if (focusedItems.isNotEmpty()) {
                add(
                    HomeSmartShelf(
                        key = "taste-cluster-$genreFocus",
                        title = "${genreFocus.replaceFirstChar { char -> char.uppercase() }} Cluster",
                        subtitle = "A focused shelf shaped by repeated taste signals.",
                        signal = "Taste",
                        items = focusedItems,
                    ),
                )
            }
        }
    }
        .distinctBy(HomeSmartShelf::key)
        .take(HomeSmartShelfLimit)
}

private fun ContinueWatchingItem.toSmartShelfPreview(): MetaPreview =
    MetaPreview(
        id = parentMetaId,
        type = parentMetaType,
        name = title,
        poster = poster ?: imageUrl,
        banner = background ?: episodeThumbnail ?: imageUrl,
        logo = logo,
        releaseInfo = released,
    )

private fun String?.smartShelfRatingOrNull(): Double? {
    val normalized = this
        ?.trim()
        ?.replace(',', '.')
        ?.takeIf(String::isNotBlank)
        ?: return null
    return normalized.substringBefore('/').toDoubleOrNull()
}

private const val HomeSmartShelfLimit = 4
private const val HomeSmartShelfItemLimit = 12
