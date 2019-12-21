package eu.kanade.tachiyomi.ui.migration

import android.os.Bundle
import com.jakewharton.rxrelay.BehaviorRelay
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.util.combineLatest
import eu.kanade.tachiyomi.util.syncChaptersWithSource
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MigrationPresenter(
        private val sourceManager: SourceManager = Injekt.get(),
        private val db: DatabaseHelper = Injekt.get(),
        private val preferences: PreferencesHelper = Injekt.get()
) : BasePresenter<MigrationController>() {

    var state = ViewState()
        private set(value) {
            field = value
            stateRelay.call(value)
        }

    private val stateRelay = BehaviorRelay.create(state)

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        db.getFavoriteMangas()
                .asRxObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { state = state.copy(sourcesWithManga = findSourcesWithManga(it)) }
                .combineLatest(stateRelay.map { it.selectedSource }
                        .distinctUntilChanged(),
                        { library, source -> library to source })
                .filter { (_, source) -> source != null }
                .observeOn(Schedulers.io())
                .map { (library, source) -> libraryToMigrationItem(library, source!!.id) }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { state = state.copy(mangaForSource = it) }
                .subscribe()

        stateRelay
                // Render the view when any field other than isReplacingManga changes
                .distinctUntilChanged { t1, t2 -> t1.isReplacingManga != t2.isReplacingManga }
                .subscribeLatestCache(MigrationController::render)

        stateRelay.distinctUntilChanged { state -> state.isReplacingManga }
                .subscribeLatestCache(MigrationController::renderIsReplacingManga)
    }

    fun setSelectedSource(source: Source) {
        state = state.copy(selectedSource = source, mangaForSource = emptyList())
    }

    fun deselectSource() {
        state = state.copy(selectedSource = null, mangaForSource = emptyList())
    }

    private fun findSourcesWithManga(library: List<Manga>): List<SourceItem> {
        val header = SelectionHeader()
        return library.map { it.source }.toSet()
                .mapNotNull { if (it != LocalSource.ID) sourceManager.getOrStub(it) else null }
                .map { SourceItem(it, header) }
    }

    private fun libraryToMigrationItem(library: List<Manga>, sourceId: Long): List<MangaItem> {
        return library.filter { it.source == sourceId }.map(::MangaItem)
    }

    fun migrateManga(prevManga: Manga, manga: Manga, replace: Boolean) {
        val source = sourceManager.get(manga.source) ?: return

        state = state.copy(isReplacingManga = true)

        Observable.defer { source.fetchChapterList(manga) }
                .onErrorReturn { emptyList() }
                .doOnNext { migrateMangaInternal(source, it, prevManga, manga, replace) }
                .onErrorReturn { emptyList() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnUnsubscribe { state = state.copy(isReplacingManga = false) }
                .subscribe()
    }

    private fun migrateMangaInternal(source: Source, sourceChapters: List<SChapter>,
                                     prevManga: Manga, manga: Manga, replace: Boolean) {

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
