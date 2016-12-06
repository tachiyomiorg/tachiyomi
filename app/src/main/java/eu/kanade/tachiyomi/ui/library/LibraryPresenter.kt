package eu.kanade.tachiyomi.ui.library

import android.os.Bundle
import android.util.Pair
import com.jakewharton.rxrelay.BehaviorRelay
import com.jakewharton.rxrelay.PublishRelay
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
import eu.kanade.tachiyomi.util.isNullOrUnsubscribed
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import uy.kohesive.injekt.injectLazy
import java.io.IOException
import java.io.InputStream
import java.util.*

/**
 * Presenter of [LibraryFragment].
 */
class LibraryPresenter : BasePresenter<LibraryFragment>() {

    /**
     * Database.
     */
    private val db: DatabaseHelper by injectLazy()

    /**
     * Preferences.
     */
    private val preferences: PreferencesHelper by injectLazy()

    /**
     * Cover cache.
     */
    private val coverCache: CoverCache by injectLazy()

    /**
     * Source manager.
     */
    private val sourceManager: SourceManager by injectLazy()

    /**
     * Download manager.
     */
    private val downloadManager: DownloadManager by injectLazy()

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
    val searchSubject: BehaviorRelay<String> = BehaviorRelay.create()

    /**
     * Subject to notify the library's viewpager for updates.
     */
    val libraryMangaSubject: BehaviorRelay<LibraryMangaEvent> = BehaviorRelay.create()

    /**
     * Subject to notify the UI of selection updates.
     */
    val selectionSubject: PublishRelay<LibrarySelectionEvent> = PublishRelay.create()

    /**
     * Relay used to apply the UI filters to the last emission of the library.
     */
    private val updateTriggerRelay = BehaviorRelay.create(Unit)

    /**
     * Library subscription.
     */
    private var librarySubscription: Subscription? = null

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        subscribeLibrary()
    }

    /**
     * Subscribes to library if needed.
     */
    fun subscribeLibrary() {
        if (librarySubscription.isNullOrUnsubscribed()) {
            librarySubscription = Observable.combineLatest(getLibraryObservable(),
                    updateTriggerRelay.observeOn(Schedulers.io()),
                    { library, updateTrigger -> library })
                    .map { Pair(it.first, applyFilters(it.second)) }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeLatestCache(
                            { view, pair -> view.onNextLibraryUpdate(pair.first, pair.second) })
        }
    }

    private fun applyFilters(map: Map<Int, List<Manga>>): Map<Int, List<Manga>> {
        val isAscending = preferences.librarySortingAscending().getOrDefault()
        val comparator = Comparator<Manga> { m1, m2 -> sortManga(m1, m2) }

        return map.mapValues { entry -> entry.value
                .filter { filterManga(it) }
                .sortedWith(if (isAscending) comparator else Collections.reverseOrder(comparator))
        }
    }

    /**
     * Get the categories and all its manga from the database.
     *
     * @return an observable of the categories and its manga.
     */
    private fun getLibraryObservable(): Observable<Pair<List<Category>, Map<Int, List<Manga>>>> {
        return Observable.combineLatest(getCategoriesObservable(), getLibraryMangasObservable(),
                { dbCategories, libraryManga ->
                    val categories = if (libraryManga.containsKey(0))
                        arrayListOf(Category.createDefault()) + dbCategories
                    else
                        dbCategories

                    this.categories = categories
                    Pair(categories, libraryManga)
                })
    }

    /**
     * Get the categories from the database.
     *
     * @return an observable of the categories.
     */
    private fun getCategoriesObservable(): Observable<List<Category>> {
        return db.getCategories().asRxObservable()
    }

    /**
     * Get the manga grouped by categories.
     *
     * @return an observable containing a map with the category id as key and a list of manga as the
     * value.
     */
    private fun getLibraryMangasObservable(): Observable<Map<Int, List<Manga>>> {
        return db.getLibraryMangas().asRxObservable()
                .map { list -> list.groupBy { it.category } }
    }

    /**
     * Requests the library to be filtered.
     */
    fun requestLibraryUpdate() {
        updateTriggerRelay.call(Unit)
    }

    /**
     * Compares the two manga determined by sorting mode.
     * Returns zero if this object is equal to the specified other object,
     * a negative number if it's less than other, or a positive number if it's greater than other.
     *
     * @param manga1 first manga to compare
     * @param manga2 second manga to compare
     */
    fun sortManga(manga1: Manga, manga2: Manga): Int {
        when (preferences.librarySortingMode().getOrDefault()) {
            LibrarySort.ALPHA -> return manga1.title.compareTo(manga2.title)
            LibrarySort.LAST_READ -> {
                var a = 0L
                var b = 0L
                db.getLastHistoryByMangaId(manga1.id!!).executeAsBlocking()?.let { a = it.last_read }
                db.getLastHistoryByMangaId(manga2.id!!).executeAsBlocking()?.let { b = it.last_read }
                return b.compareTo(a)
            }
            LibrarySort.LAST_UPDATED -> return manga2.last_update.compareTo(manga1.last_update)
            else -> return manga1.title.compareTo(manga2.title)
        }
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
        librarySubscription?.let { remove(it) }
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
            selectionSubject.call(LibrarySelectionEvent.Selected(manga))
        } else {
            selectedMangas.remove(manga)
            selectionSubject.call(LibrarySelectionEvent.Unselected(manga))
        }
    }

    /**
     * Clears all the manga selections and notifies the UI.
     */
    fun clearSelections() {
        selectedMangas.clear()
        selectionSubject.call(LibrarySelectionEvent.Cleared())
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

}
