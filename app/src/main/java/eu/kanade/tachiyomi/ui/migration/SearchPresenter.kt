package eu.kanade.tachiyomi.ui.migration

import android.os.Bundle
import com.jakewharton.rxrelay.BehaviorRelay
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.source.global_search.GlobalSearchCardItem
import eu.kanade.tachiyomi.ui.source.global_search.GlobalSearchItem
import eu.kanade.tachiyomi.ui.source.global_search.GlobalSearchPresenter
import eu.kanade.tachiyomi.util.chapter.syncChaptersWithSource
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

class SearchPresenter(
    initialQuery: String? = "",
    private val manga: Manga
) : GlobalSearchPresenter(initialQuery) {

    private val replacingMangaRelay = BehaviorRelay.create<Boolean>()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        replacingMangaRelay.subscribeLatestCache({ controller, isReplacingManga -> (controller as? SearchController)?.renderIsReplacingManga(isReplacingManga) })
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

        replacingMangaRelay.call(true)

        Observable.defer { source.fetchChapterList(manga) }
                .onErrorReturn { emptyList() }
                .doOnNext { migrateMangaInternal(source, it, prevManga, manga, replace) }
                .onErrorReturn { emptyList() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnUnsubscribe { replacingMangaRelay.call(false) }
                .subscribe()
    }

    private fun migrateMangaInternal(
        source: Source,
        sourceChapters: List<SChapter>,
        prevManga: Manga,
        manga: Manga,
        replace: Boolean
    ) {
        val flags = preferences.migrateFlags().getOrDefault()
        val migrateChapters = MigrationFlags.hasChapters(flags)
        val migrateCategories = MigrationFlags.hasCategories(flags)
        val migrateTracks = MigrationFlags.hasTracks(flags)

        db.inTransaction {
            // Update chapters read
            if (migrateChapters) {
                try {
                    syncChaptersWithSource(db, sourceChapters, manga, source)
                } catch (e: Exception) {
                    // Worst case, chapters won't be synced
                }

                val prevMangaChapters = db.getChapters(prevManga).executeAsBlocking()
                val maxChapterRead = prevMangaChapters.filter { it.read }
                        .maxBy { it.chapter_number }?.chapter_number
                if (maxChapterRead != null) {
                    val dbChapters = db.getChapters(manga).executeAsBlocking()
                    for (chapter in dbChapters) {
                        if (chapter.isRecognizedNumber && chapter.chapter_number <= maxChapterRead) {
                            chapter.read = true
                        }
                    }
                    db.insertChapters(dbChapters).executeAsBlocking()
                }
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

            // SearchPresenter#networkToLocalManga may have updated the manga title, so ensure db gets updated title
            db.updateMangaTitle(manga).executeAsBlocking()
        }
    }
}
