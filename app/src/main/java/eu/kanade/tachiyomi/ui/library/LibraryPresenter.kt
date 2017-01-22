package eu.kanade.tachiyomi.ui.library

import android.os.Bundle
import android.util.Pair
import com.hippo.unifile.UniFile
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
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.util.combineLatest
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
    private val filterTriggerRelay = BehaviorRelay.create(Unit)

    /**
     * Relay used to apply the selected sorting method to the last emission of the library.
     */
    private val sortTriggerRelay = BehaviorRelay.create(Unit)

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
            librarySubscription = getLibraryObservable()
                    .combineLatest(filterTriggerRelay.observeOn(Schedulers.io()),
                            { lib, tick -> Pair(lib.first, applyFilters(lib.second)) })
                    .combineLatest(sortTriggerRelay.observeOn(Schedulers.io()),
                            { lib, tick -> Pair(lib.first, applySort(lib.second)) })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeLatestCache({ view, pair ->
                        view.onNextLibraryUpdate(pair.first, pair.second)
                    })
        }
    }

    /**
     * Applies library filters to the given map of manga.
     *
     * @param map the map to filter.
     */
    private fun applyFilters(map: Map<Int, List<Manga>>): Map<Int, List<Manga>> {
        // Cached list of downloaded manga directories given a source id.
        val mangaDirectories = mutableMapOf<Long, Array<UniFile>>()

        // Cached list of downloaded chapter directories for a manga.
        val chapterDirectories = mutableMapOf<Long, Boolean>()

        val filterDownloaded = preferences.filterDownloaded().getOrDefault()

        val filterUnread = preferences.filterUnread().getOrDefault()

        val filterFn: (Manga) -> Boolean = f@ { manga ->
            // Filter out manga without source.
            val source = sourceManager.get(manga.source) ?: return@f false

            // Filter when there isn't unread chapters.
            if (filterUnread && manga.unread == 0) {
                return@f false
            }

            // Filter when the download directory doesn't exist or is null.
            if (filterDownloaded) {
                val mangaDirs = mangaDirectories.getOrPut(source.id) {
                    downloadManager.findSourceDir(source)?.listFiles() ?: emptyArray()
                }

                val mangaDirName = downloadManager.getMangaDirName(manga)
                val mangaDir = mangaDirs.find { it.name == mangaDirName } ?: return@f false

                val hasDirs = chapterDirectories.getOrPut(manga.id!!) {
                    (mangaDir.listFiles() ?: emptyArray()).isNotEmpty()
                }
                if (!hasDirs) {
                    return@f false
                }
            }
            true
        }

        return map.mapValues { entry -> entry.value.filter(filterFn) }
    }

    /**
     * Applies library sorting to the given map of manga.
     *
     * @param map the map to sort.
     */
    private fun applySort(map: Map<Int, List<Manga>>): Map<Int, List<Manga>> {
        val sortingMode = preferences.librarySortingMode().getOrDefault()

        // TODO lazy initialization in kotlin 1.1
        var lastReadManga: Map<Long, Int>? = null
        if (sortingMode == LibrarySort.LAST_READ) {
            var counter = 0
            lastReadManga = db.getLastReadManga().executeAsBlocking()
                    .associate { it.id!! to counter++ }
        }

        val sortFn: (Manga, Manga) -> Int = { manga1, manga2 ->
            when (sortingMode) {
                LibrarySort.ALPHA -> manga1.title.compareTo(manga2.title)
                LibrarySort.LAST_READ -> {
                    // Get index of manga, set equal to list if size unknown.
                    val manga1LastRead = lastReadManga!![manga1.id!!] ?: lastReadManga!!.size
                    val manga2LastRead = lastReadManga!![manga2.id!!] ?: lastReadManga!!.size
                    manga1LastRead.compareTo(manga2LastRead)
                }
                LibrarySort.LAST_UPDATED -> manga2.last_update.compareTo(manga1.last_update)
                LibrarySort.UNREAD -> manga1.unread.compareTo(manga2.unread)
                else -> throw Exception("Unknown sorting mode")
            }
        }

        val comparator = if (preferences.librarySortingAscending().getOrDefault())
            Comparator(sortFn)
        else
            Collections.reverseOrder(sortFn)

        return map.mapValues { entry -> entry.value.sortedWith(comparator) }
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
    fun requestFilterUpdate() {
        filterTriggerRelay.call(Unit)
    }

    /**
     * Requests the library to be sorted.
     */
    fun requestSortUpdate() {
        sortTriggerRelay.call(Unit)
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
    fun getCommonCategories(mangas: List<Manga>): Collection<Category> {
        if (mangas.isEmpty()) return emptyList()
        return mangas.toSet()
                .map { db.getCategoriesForManga(it).executeAsBlocking() }
                .reduce { set1: Iterable<Category>, set2 -> set1.intersect(set2) }
    }

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
