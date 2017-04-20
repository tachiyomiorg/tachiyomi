package eu.kanade.tachiyomi.ui.manga2.info

import android.os.Bundle
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.util.SharedData
import eu.kanade.tachiyomi.util.isNullOrUnsubscribed
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import uy.kohesive.injekt.injectLazy

/**
 * Presenter of MangaInfoFragment.
 * Contains information and data for fragment.
 * Observable updates should be called from here.
 */
class MangaInfoPresenter(var manga: Manga) : BasePresenter<MangaInfoController>() {

    /**
     * Source of the manga.
     */
    lateinit var source: Source
        private set

    /**
     * Used to connect to database.
     */
    val db: DatabaseHelper by injectLazy()

    /**
     * Used to connect to different manga sources.
     */
    val sourceManager: SourceManager by injectLazy()

    /**
     * Used to connect to cache.
     */
    val coverCache: CoverCache by injectLazy()

    private val downloadManager: DownloadManager by injectLazy()

    /**
     * Subscription to send the manga to the view.
     */
    private var viewMangaSubcription: Subscription? = null

    /**
     * Subscription to update the manga from the source.
     */
    private var fetchMangaSubscription: Subscription? = null

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

//        manga = SharedData.get(MangaEvent::class.java)?.manga ?: return
        source = sourceManager.get(manga.source)!!
        sendMangaToView()

        // Update chapter count
        SharedData.get(ChapterCountEvent::class.java)?.observable
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribeLatestCache(MangaInfoController::setChapterCount)

        // Update favorite status
        SharedData.get(MangaFavoriteEvent::class.java)?.let {
            it.observable
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { setFavorite(it) }
                    .apply { add(this) }
        }
    }

    /**
     * Sends the active manga to the view.
     */
    fun sendMangaToView() {
        viewMangaSubcription?.let { remove(it) }
        viewMangaSubcription = Observable.just(manga)
                .subscribeLatestCache({ view, manga -> view.onNextManga(manga, source) })
    }

    /**
     * Fetch manga information from source.
     */
    fun fetchMangaFromSource() {
        if (!fetchMangaSubscription.isNullOrUnsubscribed()) return
        fetchMangaSubscription = Observable.defer { source.fetchMangaDetails(manga) }
                .map { networkManga ->
                    manga.copyFrom(networkManga)
                    manga.initialized = true
                    db.insertManga(manga).executeAsBlocking()
                    manga
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { sendMangaToView() }
                .subscribeFirst({ view, manga ->
                    view.onFetchMangaDone()
                }, { view, error ->
                    view.onFetchMangaError()
                })
    }

    /**
     * Update favorite status of manga, (removes / adds) manga (to / from) library.
     *
     * @return the new status of the manga.
     */
    fun toggleFavorite(): Boolean {
        manga.favorite = !manga.favorite
        if (!manga.favorite) {
            coverCache.deleteFromCache(manga.thumbnail_url)
        }
        db.insertManga(manga).executeAsBlocking()
        sendMangaToView()
        return manga.favorite
    }

    private fun setFavorite(favorite: Boolean) {
        if (manga.favorite == favorite) {
            return
        }
        toggleFavorite()
    }

    /**
     * Returns true if the manga has any downloads.
     */
    fun hasDownloads(): Boolean {
        return downloadManager.findMangaDir(source, manga) != null
    }

    /**
     * Deletes all the downloads for the manga.
     */
    fun deleteDownloads() {
        downloadManager.findMangaDir(source, manga)?.delete()
    }

    /**
     * Get the default, and user categories.
     *
     * @return List of categories, default plus user categories
     */
    fun getCategories(): List<Category> {
        return db.getCategories().executeAsBlocking()
    }

    /**
     * Gets the category id's the manga is in, if the manga is not in a category, returns the default id.
     *
     * @param manga the manga to get categories from.
     * @return Array of category ids the manga is in, if none returns default id
     */
    fun getMangaCategoryIds(manga: Manga): Array<Int> {
        val categories = db.getCategoriesForManga(manga).executeAsBlocking()
        return categories.mapNotNull { it.id }.toTypedArray()
    }

    /**
     * Move the given manga to categories.
     *
     * @param manga the manga to move.
     * @param categories the selected categories.
     */
    fun moveMangaToCategories(manga: Manga, categories: List<Category>) {
        val mc = categories.filter { it.id != 0 }.map { MangaCategory.create(manga, it) }
        db.setMangaCategories(mc, listOf(manga))
    }

    /**
     * Move the given manga to the category.
     *
     * @param manga the manga to move.
     * @param category the selected category, or null for default category.
     */
    fun moveMangaToCategory(manga: Manga, category: Category?) {
        moveMangaToCategories(manga, listOfNotNull(category))
    }

}
