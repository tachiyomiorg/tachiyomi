package eu.kanade.tachiyomi.ui.browse.migration.search

import android.os.Bundle
import com.jakewharton.rxrelay.BehaviorRelay
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.database.models.toMangaInfo
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.toSChapter
import eu.kanade.tachiyomi.ui.browse.migration.MigrationFlags
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchCardItem
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchItem
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchPresenter
import eu.kanade.tachiyomi.util.chapter.syncChaptersWithSource
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.launchUI
import eu.kanade.tachiyomi.util.lang.withUIContext
import eu.kanade.tachiyomi.util.system.toast
import java.util.Date

class SearchPresenter(
    initialQuery: String? = "",
    private val manga: Manga
) : GlobalSearchPresenter(initialQuery) {

    private val replacingMangaRelay = BehaviorRelay.create<Pair<Boolean, Manga?>>()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        replacingMangaRelay.subscribeLatestCache(
            { controller, (isReplacingManga, newManga) ->
                (controller as? SearchController)?.renderIsReplacingManga(isReplacingManga, newManga)
            }
        )
    }

    override fun getEnabledSources(): List<CatalogueSource> {
        // Put the source of the selected manga at the top
        return super.getEnabledSources()
            .sortedByDescending { it.id == manga.source }
    }

    override fun createCatalogueSearchItem(source: CatalogueSource, results: List<GlobalSearchCardItem>?): GlobalSearchItem {
        // Set the catalogue search item as highlighted if the source matches that of the selected manga
        return GlobalSearchItem(source, results, source.id == manga.source)
    }

    override fun networkToLocalManga(sManga: SManga, sourceId: Long): Manga {
        val localManga = super.networkToLocalManga(sManga, sourceId)
        // For migration, displayed title should always match source rather than local DB
        localManga.title = sManga.title
        return localManga
    }

    fun migrateManga(prevManga: Manga, manga: Manga, replace: Boolean) {
        val source = sourceManager.get(manga.source) ?: return

        replacingMangaRelay.call(Pair(true, null))

        presenterScope.launchIO {
            try {
                val chapters = source.getChapterList(manga.toMangaInfo())
                    .map { it.toSChapter() }

                migrateMangaInternal(source, chapters, prevManga, manga, replace)
            } catch (e: Throwable) {
                withUIContext { view?.applicationContext?.toast(e.message) }
            }

            presenterScope.launchUI { replacingMangaRelay.call(Pair(false, manga)) }
        }
    }

    private fun migrateMangaInternal(
        source: Source,
        sourceChapters: List<SChapter>,
        prevManga: Manga,
        manga: Manga,
        replace: Boolean
    ) {
        val flags = preferences.migrateFlags().get()
        val migrateChapters =
            MigrationFlags.hasChapters(
                flags
            )
        val migrateCategories =
            MigrationFlags.hasCategories(
                flags
            )
        val migrateTracks =
            MigrationFlags.hasTracks(
                flags
            )

        db.inTransaction {
            // Update chapters read
            if (migrateChapters) {
                try {
                    syncChaptersWithSource(db, sourceChapters, manga, source)
                } catch (e: Exception) {
                    // Worst case, chapters won't be synced
                }

                val prevMangaChapters = db.getChapters(prevManga).executeAsBlocking()
                val maxChapterRead = prevMangaChapters
                    .filter { it.read }
                    .maxOfOrNull { it.chapter_number } ?: 0f
                val dbChapters = db.getChapters(manga).executeAsBlocking()
                for (chapter in dbChapters) {
                    if (chapter.isRecognizedNumber) {
                        val prevChapter = prevMangaChapters
                            .find { it.isRecognizedNumber && it.chapter_number == chapter.chapter_number }
                        if (prevChapter != null) {
                            chapter.date_fetch = prevChapter.date_fetch
                            chapter.bookmark = prevChapter.bookmark
                        }
                        if (chapter.chapter_number <= maxChapterRead) {
                            chapter.read = true
                        }
                    }
                }
                db.insertChapters(dbChapters).executeAsBlocking()
            }

            // Update categories
            if (migrateCategories) {
                val categories = db.getCategoriesForManga(prevManga).executeAsBlocking()
                val mangaCategories = categories.map { MangaCategory.create(manga, it) }
                db.setMangaCategories(mangaCategories, listOf(manga))
            }

            // Update track
            if (migrateTracks) {
                val tracks = db.getTracks(prevManga).executeAsBlocking()
                for (track in tracks) {
                    track.id = null
                    track.manga_id = manga.id!!
                }
                db.insertTracks(tracks).executeAsBlocking()
            }

            // Update favorite status
            if (replace) {
                prevManga.favorite = false
                db.updateMangaFavorite(prevManga).executeAsBlocking()
            }
            manga.favorite = true
            db.updateMangaFavorite(manga).executeAsBlocking()

            // Update reading preferences
            manga.chapter_flags = prevManga.chapter_flags
            db.updateChapterFlags(manga).executeAsBlocking()
            manga.viewer_flags = prevManga.viewer_flags
            db.updateViewerFlags(manga).executeAsBlocking()

            // Update date added
            if (replace) {
                manga.date_added = prevManga.date_added
                prevManga.date_added = 0
            } else {
                manga.date_added = Date().time
            }

            // SearchPresenter#networkToLocalManga may have updated the manga title, so ensure db gets updated title
            db.updateMangaTitle(manga).executeAsBlocking()
        }
    }
}
