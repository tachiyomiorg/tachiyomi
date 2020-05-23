package eu.kanade.tachiyomi.ui.browse.migration

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.browse.migration.manga.MangaItem
import eu.kanade.tachiyomi.ui.browse.migration.sources.SourceItem

data class ViewState(
    val selectedSource: Source? = null,
    val mangaForSource: List<MangaItem> = emptyList(),
    val sourcesWithManga: List<SourceItem> = emptyList()
)
