package eu.kanade.tachiyomi.ui.migration

import eu.kanade.tachiyomi.source.Source

data class ViewState(
        val selectedSource: Source? = null,
        val mangaForSource: List<MangaItem> = emptyList(),
        val sourcesWithManga: List<SourceItem> = emptyList(),
        val isReplacingManga: Boolean = false
)