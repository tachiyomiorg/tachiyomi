package eu.kanade.tachiyomi.ui.manga.info

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
import eu.kanade.tachiyomi.ui.manga.MangaEvent
import eu.kanade.tachiyomi.util.SharedData
import eu.kanade.tachiyomi.util.isNullOrUnsubscribed
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import uy.kohesive.injekt.injectLazy
import java.util.ArrayList

/**
 * Presenter of MangaInfoFragment.
 * Contains information and data for fragment.
 * Observable updates should be called from here.
 */
class MangaInfoPresenter : BasePresenter<MangaInfoFragment>() {

    /**
     * Active manga.
     */
    lateinit var manga: Manga
        private set

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

        manga = SharedData.get(MangaEvent::class.java)?.manga ?: return
        source = sourceManager.get(manga.source)!!
        sendMangaToView()

        // Update chapter count
        SharedData.get(ChapterCountEvent::class.java)?.observable
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribeLatestCache(MangaInfoFragment::setChapterCount)

        // Update favorite status
        SharedData.get(MangaFavoriteEvent::class.java)?.observable
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe { setFavorite(it) }
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

    fun getCategories(): List<Category> {
        return arrayListOf(Category.createDefault()) + db.getCategories().executeAsBlocking()
    }

    fun getMangaCategoryIds(manga: Manga): Array<Int?> {
        val categories = db.getCategoriesForManga(manga).executeAsBlocking()
        if(categories.isEmpty()) {
            return arrayListOf(Category.createDefault().id).toTypedArray()
        }
        return db.getCategoriesForManga(manga).executeAsBlocking().map { it.id }.toTypedArray()
    }

    /**
     * Move the given manga to categories.
     *
     * @param categories the selected categories.
     * @param manga the manga to move.
     */
    fun moveMangaToCategories(categories: List<Category>, manga: Manga) {
        val mc = ArrayList<MangaCategory>()

        for (cat in categories) {
            mc.add(MangaCategory.create(manga, cat))
        }

        db.setMangaCategories(mc, arrayListOf(manga))
    }

    fun moveMangaToCategory(category: Category, manga: Manga) {
        moveMangaToCategories(arrayListOf(category), manga)
    }

}
