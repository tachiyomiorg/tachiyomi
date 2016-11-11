package eu.kanade.tachiyomi.ui.library

import android.os.Bundle
import android.util.Pair
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.data.source.SourceManager
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subjects.BehaviorSubject
import rx.subjects.PublishSubject
import uy.kohesive.injekt.injectLazy
import java.io.IOException
import java.io.InputStream
import java.util.*

/**
 * Presenter of [LibraryFragment].
 */
class LibraryPresenter : BasePresenter<LibraryFragment>() {

    /**
     * Categories of the library.
     */
    var categories: List<Category> = emptyList()

    /**
     * Currently selected manga.
     */
    val selectedMangas = mutableListOf<Manga>()

    /**
     * Search query of the library.
     */
    val searchSubject: BehaviorSubject<String> = BehaviorSubject.create()

    /**
     * Subject to notify the library's viewpager for updates.
     */
    val libraryMangaSubject: BehaviorSubject<LibraryMangaEvent> = BehaviorSubject.create()

    /**
     * Subject to notify the UI of selection updates.
     */
    val selectionSubject: PublishSubject<LibrarySelectionEvent> = PublishSubject.create()

    /**
     * Database.
     */
    val db: DatabaseHelper by injectLazy()

    /**
     * Preferences.
     */
    val preferences: PreferencesHelper by injectLazy()

    /**
     * Cover cache.
     */
    val coverCache: CoverCache by injectLazy()

    /**
     * Source manager.
     */
    val sourceManager: SourceManager by injectLazy()

    /**
     * Download manager.
     */
    val downloadManager: DownloadManager by injectLazy()

    companion object {
        /**
         * Id of the restartable that listens for library updates.
         */
        const val GET_LIBRARY = 1
    }

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        restartableLatestCache(GET_LIBRARY,
                { getLibraryObservable() },
                { view, pair -> view.onNextLibraryUpdate(pair.first, pair.second) })

        if (savedState == null) {
            start(GET_LIBRARY)
        }

    }

    /**
     * Get the categories and all its manga from the database.
     *
     * @return an observable of the categories and its manga.
     */
    fun getLibraryObservable(): Observable<Pair<List<Category>, Map<Int, List<Manga>>>> {
        return Observable.combineLatest(getCategoriesObservable(), getLibraryMangasObservable(),
                { dbCategories, libraryManga ->
                    val categories = if (libraryManga.containsKey(0))
                        arrayListOf(Category.createDefault()) + dbCategories
                    else
                        dbCategories

                    this.categories = categories
                    Pair(categories, libraryManga)
                })
                .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * Get the categories from the database.
     *
     * @return an observable of the categories.
     */
    fun getCategoriesObservable(): Observable<List<Category>> {
        return db.getCategories().asRxObservable()
    }

    /**
     * Get the manga grouped by categories.
     *
     * @return an observable containing a map with the category id as key and a list of manga as the
     * value.
     */
    fun getLibraryMangasObservable(): Observable<Map<Int, List<Manga>>> {
        return db.getLibraryMangas().asRxObservable()
                .flatMap { mangas ->
                    Observable.from(mangas)
                            // Filter library by options
                            .filter { filterManga(it) }
                            .groupBy { it.category }
                            .flatMap { group -> group.toList().map { Pair(group.key, it) } }
                            .toMap({ it.first }, { it.second })
                }
    }

    /**
     * Resubscribes to library if needed.
     */
    fun subscribeLibrary() {
        if (isUnsubscribed(GET_LIBRARY)) {
            start(GET_LIBRARY)
        }
    }

    /**
     * Resubscribes to library.
     */
    fun resubscribeLibrary() {
        start(GET_LIBRARY)
    }

    /**
     * Filters an entry of the library.
     *
     * @param manga a favorite manga from the database.
     * @return true if the entry is included, false otherwise.
     */
    fun filterManga(manga: Manga): Boolean {
        // Filter out manga without source
        val source = sourceManager.get(manga.source) ?: return false

        val prefFilterDownloaded = preferences.filterDownloaded().getOrDefault()
        val prefFilterUnread = preferences.filterUnread().getOrDefault()

        // Check if filter option is selected
        if (prefFilterDownloaded || prefFilterUnread) {

            // Does it have downloaded chapters.
            var hasDownloaded = false
            var hasUnread = false

            if (prefFilterUnread) {
                // Does it have unread chapters.
                hasUnread = manga.unread > 0
            }

            if (prefFilterDownloaded) {
                val mangaDir = downloadManager.findMangaDir(source, manga)

                if (mangaDir != null) {
                    hasDownloaded = mangaDir.listFiles()?.any { it.isDirectory } ?: false
                }
            }

            // Return correct filter status
            if (prefFilterDownloaded && prefFilterUnread) {
                return (hasDownloaded && hasUnread)
            } else {
                return (hasDownloaded || hasUnread)
            }
        } else {
            return true
        }
    }

    /**
     * Called when a manga is opened.
     */
    fun onOpenManga() {
        // Avoid further db updates for the library when it's not needed
        stop(GET_LIBRARY)
    }

    /**
     * Sets the selection for a given manga.
     *
     * @param manga the manga whose selection has changed.
     * @param selected whether it's now selected or not.
     */
    fun setSelection(manga: Manga, selected: Boolean) {
        if (selected) {
            selectedMangas.add(manga)
            selectionSubject.onNext(LibrarySelectionEvent.Selected(manga))
        } else {
            selectedMangas.remove(manga)
            selectionSubject.onNext(LibrarySelectionEvent.Unselected(manga))
        }
    }

    /**
     * Clears all the manga selections and notifies the UI.
     */
    fun clearSelections() {
        selectedMangas.clear()
        selectionSubject.onNext(LibrarySelectionEvent.Cleared())
    }

    /**
     * Returns the common categories for the given list of manga.
     *
     * @param mangas the list of manga.
     */
    fun getCommonCategories(mangas: List<Manga>): Collection<Category> = mangas.toSet()
            .map { db.getCategoriesForManga(it).executeAsBlocking() }
            .reduce { set1: Iterable<Category>, set2 -> set1.intersect(set2) }

    /**
     * Remove the selected manga from the library.
     */
    fun removeMangaFromLibrary() {
        // Create a set of the list
        val mangaToDelete = selectedMangas.toSet()

        Observable.from(mangaToDelete)
                .subscribeOn(Schedulers.io())
                .doOnNext {
                    it.favorite = false
                    coverCache.deleteFromCache(it.thumbnail_url)
                }
                .toList()
                .flatMap { db.insertMangas(it).asRxObservable() }
                .subscribe()
    }

    /**
     * Move the given list of manga to categories.
     *
     * @param categories the selected categories.
     * @param mangas the list of manga to move.
     */
    fun moveMangasToCategories(categories: List<Category>, mangas: List<Manga>) {
        val mc = ArrayList<MangaCategory>()

        for (manga in mangas) {
            for (cat in categories) {
                mc.add(MangaCategory.create(manga, cat))
            }
        }

        db.setMangaCategories(mc, mangas)
    }

    /**
     * Update cover with local file.
     *
     * @param inputStream the new cover.
     * @param manga the manga edited.
     * @return true if the cover is updated, false otherwise
     */
    @Throws(IOException::class)
    fun editCoverWithStream(inputStream: InputStream, manga: Manga): Boolean {
        if (manga.thumbnail_url != null && manga.favorite) {
            coverCache.copyToCache(manga.thumbnail_url!!, inputStream)
            return true
        }
        return false
    }

    /**
     * Changes the active display mode.
     */
    fun swapDisplayMode() {
        val displayAsList = preferences.libraryAsList().getOrDefault()
        preferences.libraryAsList().set(!displayAsList)
    }

}
