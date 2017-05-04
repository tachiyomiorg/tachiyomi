package eu.kanade.tachiyomi.ui.manga.info

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.view.*
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.BitmapRequestBuilder
import com.bumptech.glide.BitmapTypeRequest
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.jakewharton.rxbinding.support.v4.widget.refreshes
import com.jakewharton.rxbinding.view.clicks
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.library.ChangeMangaCategoriesDialog
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.manga.MangaController
import eu.kanade.tachiyomi.util.getResourceColor
import eu.kanade.tachiyomi.util.snack
import eu.kanade.tachiyomi.util.toast
import jp.wasabeef.glide.transformations.CropCircleTransformation
import jp.wasabeef.glide.transformations.CropSquareTransformation
import jp.wasabeef.glide.transformations.MaskTransformation
import jp.wasabeef.glide.transformations.RoundedCornersTransformation
import kotlinx.android.synthetic.main.fragment_manga_info.view.*
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.Subscriptions
import uy.kohesive.injekt.injectLazy

/**
 * Fragment that shows manga information.
 * Uses R.layout.fragment_manga_info.
 * UI related actions should be called from here.
 */
class MangaInfoController : NucleusController<MangaInfoPresenter>(),
        ChangeMangaCategoriesDialog.Listener {

    /**
     * Preferences helper.
     */
    private val preferences: PreferencesHelper by injectLazy()

    init {
        setHasOptionsMenu(true)
        setOptionsMenuHidden(true)
    }

    override fun createPresenter(): MangaInfoPresenter {
        val ctrl = parentController as MangaController
        return MangaInfoPresenter(ctrl.manga!!, ctrl.source!!,
                ctrl.chapterCountRelay, ctrl.mangaFavoriteRelay)
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.fragment_manga_info, container, false)
    }

    override fun onViewCreated(view: View, savedViewState: Bundle?) {
        super.onViewCreated(view, savedViewState)

        with(view) {
            // Set onclickListener to toggle favorite when FAB clicked.
            fab_favorite.clicks().subscribeUntilDestroy { onFabClick() }

            // Set SwipeRefresh to refresh manga data.
            swipe_refresh.refreshes().subscribeUntilDestroy { fetchMangaFromSource() }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.manga_info, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_open_in_browser -> openInBrowser()
            R.id.action_share -> shareManga()
            R.id.action_add_to_home_screen -> addToHomeScreen()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    /**
     * Check if manga is initialized.
     * If true update view with manga information,
     * if false fetch manga information
     *
     * @param manga  manga object containing information about manga.
     * @param source the source of the manga.
     */
    fun onNextManga(manga: Manga, source: Source) {
        if (manga.initialized) {
            // Update view.
            setMangaInfo(manga, source)
        } else {
            // Initialize manga.
            fetchMangaFromSource()
        }
    }

    /**
     * Update the view with manga information.
     *
     * @param manga manga object containing information about manga.
     * @param source the source of the manga.
     */
    private fun setMangaInfo(manga: Manga, source: Source?) {
        val view = view ?: return
        with(view) {
            // Update artist TextView.
            manga_artist.text = manga.artist

            // Update author TextView.
            manga_author.text = manga.author

            // If manga source is known update source TextView.
            if (source != null) {
                manga_source.text = source.toString()
            }

            // Update genres TextView.
            manga_genres.text = manga.genre

            // Update status TextView.
            manga_status.setText(when (manga.status) {
                SManga.ONGOING -> R.string.ongoing
                SManga.COMPLETED -> R.string.completed
                SManga.LICENSED -> R.string.licensed
                else -> R.string.unknown
            })

            // Update description TextView.
            manga_summary.text = manga.description

            // Set the favorite drawable to the correct one.
            setFavoriteDrawable(manga.favorite)

            // Set cover if it wasn't already.
            if (manga_cover.drawable == null && !manga.thumbnail_url.isNullOrEmpty()) {
                Glide.with(context)
                        .load(manga)
                        .diskCacheStrategy(DiskCacheStrategy.RESULT)
                        .centerCrop()
                        .into(manga_cover)

                Glide.with(context)
                        .load(manga)
                        .diskCacheStrategy(DiskCacheStrategy.RESULT)
                        .centerCrop()
                        .into(backdrop)
            }
        }
    }

    /**
     * Update chapter count TextView.
     *
     * @param count number of chapters.
     */
    fun setChapterCount(count: Int) {
        view?.manga_chapters?.text = count.toString()
    }

    /**
     * Toggles the favorite status and asks for confirmation to delete downloaded chapters.
     */
    fun toggleFavorite() {
        val view = view

        val isNowFavorite = presenter.toggleFavorite()
        if (view != null && !isNowFavorite && presenter.hasDownloads()) {
            view.snack(view.context.getString(R.string.delete_downloads_for_manga)) {
                setAction(R.string.action_delete) {
                    presenter.deleteDownloads()
                }
            }
        }
    }

    /**
     * Open the manga in browser.
     */
    fun openInBrowser() {
        val context = view?.context ?: return
        val source = presenter.source as? HttpSource ?: return

        try {
            val url = Uri.parse(source.mangaDetailsRequest(presenter.manga).url().toString())
            val intent = CustomTabsIntent.Builder()
                    .setToolbarColor(context.getResourceColor(R.attr.colorPrimary))
                    .build()
            intent.launchUrl(activity, url)
        } catch (e: Exception) {
            context.toast(e.message)
        }
    }

    /**
     * Called to run Intent with [Intent.ACTION_SEND], which show share dialog.
     */
    private fun shareManga() {
        val context = view?.context ?: return

        val source = presenter.source as? HttpSource ?: return
        try {
            val url = source.mangaDetailsRequest(presenter.manga).url().toString()
            val title = presenter.manga.title
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_text, title, url))
            }
            startActivity(Intent.createChooser(intent, context.getString(R.string.action_share)))
        } catch (e: Exception) {
            context.toast(e.message)
        }
    }

    /**
     * Update FAB with correct drawable.
     *
     * @param isFavorite determines if manga is favorite or not.
     */
    private fun setFavoriteDrawable(isFavorite: Boolean) {
        // Set the Favorite drawable to the correct one.
        // Border drawable if false, filled drawable if true.
        view?.fab_favorite?.setImageResource(if (isFavorite)
            R.drawable.ic_bookmark_white_24dp
        else
            R.drawable.ic_bookmark_border_white_24dp)
    }

    /**
     * Start fetching manga information from source.
     */
    private fun fetchMangaFromSource() {
        setRefreshing(true)
        // Call presenter and start fetching manga information
        presenter.fetchMangaFromSource()
    }


    /**
     * Update swipe refresh to stop showing refresh in progress spinner.
     */
    fun onFetchMangaDone() {
        setRefreshing(false)
    }

    /**
     * Update swipe refresh to start showing refresh in progress spinner.
     */
    fun onFetchMangaError() {
        setRefreshing(false)
    }

    /**
     * Set swipe refresh status.
     *
     * @param value whether it should be refreshing or not.
     */
    private fun setRefreshing(value: Boolean) {
        view?.swipe_refresh?.isRefreshing = value
    }

    /**
     * Called when the fab is clicked.
     */
    private fun onFabClick() {
        val manga = presenter.manga
        toggleFavorite()
        if (manga.favorite) {
            val categories = presenter.getCategories()
            val defaultCategory = categories.find { it.id == preferences.defaultCategory() }
            if (defaultCategory != null) {
                presenter.moveMangaToCategory(manga, defaultCategory)
            } else if (categories.size <= 1) { // default or the one from the user
                presenter.moveMangaToCategory(manga, categories.firstOrNull())
            } else {
                val ids = presenter.getMangaCategoryIds(manga)
                val preselected = ids.mapNotNull { id ->
                    categories.indexOfFirst { it.id == id }.takeIf { it != -1 }
                }.toTypedArray()

                ChangeMangaCategoriesDialog(this, listOf(manga), categories, preselected)
                        .showDialog(router)
            }
        }
    }

    override fun updateCategoriesForMangas(mangas: List<Manga>, categories: List<Category>) {
        val manga = mangas.firstOrNull() ?: return
        presenter.moveMangaToCategories(manga, categories)
    }

    /**
     * Add the manga to the home screen
     */
    fun addToHomeScreen() {
        val activity = activity ?: return
        val mangaControllerArgs = parentController?.args ?: return

        val shortcutIntent = activity.intent
                .setAction(MainActivity.SHORTCUT_MANGA)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(MangaController.MANGA_EXTRA,
                        mangaControllerArgs.getLong(MangaController.MANGA_EXTRA))

        val addIntent = Intent("com.android.launcher.action.INSTALL_SHORTCUT")
                .putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)

        //Set shortcut title
        val dialog = MaterialDialog.Builder(activity)
                .title(R.string.shortcut_title)
                .input("", presenter.manga.title, { _, text ->
                    //Set shortcut title
                    addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, text.toString())

                    reshapeIconBitmap(addIntent,
                            Glide.with(activity).load(presenter.manga).asBitmap())
                })
                .negativeText(android.R.string.cancel)
                .show()

        untilDestroySubscriptions.add(Subscriptions.create { dialog.dismiss() })
    }

    fun reshapeIconBitmap(addIntent: Intent, request: BitmapTypeRequest<out Any>) {
        val activity = activity ?: return

        val modes = intArrayOf(R.string.circular_icon,
                R.string.rounded_icon,
                R.string.square_icon,
                R.string.star_icon)

        fun BitmapRequestBuilder<out Any, Bitmap>.toIcon(): Bitmap {
            return this.into(96, 96).get()
        }

        // i = 0: Circular icon
        // i = 1: Rounded icon
        // i = 2: Square icon
        // i = 3: Star icon (because boredom)
        fun getIcon(i: Int): Bitmap? {
            return when (i) {
                0 -> request.transform(CropCircleTransformation(activity)).toIcon()
                1 -> request.transform(RoundedCornersTransformation(activity, 5, 0)).toIcon()
                2 -> request.transform(CropSquareTransformation(activity)).toIcon()
                3 -> request.transform(CenterCrop(activity),
                        MaskTransformation(activity, R.drawable.mask_star)).toIcon()
                else -> null
            }
        }

        val dialog = MaterialDialog.Builder(activity)
                .title(R.string.icon_shape)
                .negativeText(android.R.string.cancel)
                .items(modes.map { activity.getString(it) })
                .itemsCallback { _, _, i, _ ->
                    Observable.fromCallable { getIcon(i) }
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({ icon ->
                                if (icon != null) createShortcut(addIntent, icon)
                            }, {
                                activity.toast(R.string.icon_creation_fail)
                            })
                }
                .show()

        untilDestroySubscriptions.add(Subscriptions.create { dialog.dismiss() })
    }

    fun createShortcut(addIntent: Intent, icon: Bitmap) {
        val activity = activity ?: return

        //Send shortcut intent
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, icon)
        activity.sendBroadcast(addIntent)
        //Go to launcher to show this shiny new shortcut!
        val startMain = Intent(Intent.ACTION_MAIN)
        startMain.addCategory(Intent.CATEGORY_HOME).flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(startMain)
    }

}
