package eu.kanade.tachiyomi.ui.manga.info

import android.app.Dialog
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.v4.content.pm.ShortcutInfoCompat
import android.support.v4.content.pm.ShortcutManagerCompat
import android.support.v4.graphics.drawable.IconCompat
import android.view.*
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.jakewharton.rxbinding.support.v4.widget.refreshes
import com.jakewharton.rxbinding.view.clicks
import com.jakewharton.rxbinding.view.longClicks
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.glide.GlideApp
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.catalogue.global_search.CatalogueSearchController
import eu.kanade.tachiyomi.ui.library.ChangeMangaCategoriesDialog
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.manga.MangaController
import eu.kanade.tachiyomi.util.getResourceColor
import eu.kanade.tachiyomi.util.openInBrowser
import eu.kanade.tachiyomi.util.snack
import eu.kanade.tachiyomi.util.toast
import eu.kanade.tachiyomi.util.truncateCenter
import jp.wasabeef.glide.transformations.CropSquareTransformation
import jp.wasabeef.glide.transformations.MaskTransformation
import kotlinx.android.synthetic.main.manga_info_controller.*
import uy.kohesive.injekt.injectLazy
import java.text.DateFormat
import java.text.DecimalFormat
import java.util.Date

/**
 * Fragment that shows manga information.
 * Uses R.layout.manga_info_controller.
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
                ctrl.chapterCountRelay, ctrl.lastUpdateRelay, ctrl.mangaFavoriteRelay)
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.manga_info_controller, container, false)
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        // Set onclickListener to toggle favorite when FAB clicked.
        fab_favorite.clicks().subscribeUntilDestroy { onFabClick() }

        // Set onLongClickListener to manage categories when FAB is clicked.
        fab_favorite.longClicks().subscribeUntilDestroy{ onFabLongClick() }

        // Set SwipeRefresh to refresh manga data.
        swipe_refresh.refreshes().subscribeUntilDestroy { fetchMangaFromSource() }

        manga_full_title.longClicks().subscribeUntilDestroy {
            copyToClipboard(view.context.getString(R.string.title), manga_full_title.text.toString())
        }

        manga_full_title.clicks().subscribeUntilDestroy {
            performGlobalSearch(manga_full_title.text.toString())
        }

        manga_artist.longClicks().subscribeUntilDestroy {
            copyToClipboard(manga_artist_label.text.toString(), manga_artist.text.toString())
        }

        manga_artist.clicks().subscribeUntilDestroy {
            performGlobalSearch(manga_artist.text.toString())
        }

        manga_author.longClicks().subscribeUntilDestroy {
            copyToClipboard(manga_author.text.toString(), manga_author.text.toString())
        }

        manga_author.clicks().subscribeUntilDestroy {
            performGlobalSearch(manga_author.text.toString())
        }

        manga_summary.longClicks().subscribeUntilDestroy {
            copyToClipboard(view.context.getString(R.string.description), manga_summary.text.toString())
        }

        //manga_genres_tags.setOnTagClickListener { tag -> performGlobalSearch(tag) }

        manga_cover.longClicks().subscribeUntilDestroy {
            copyToClipboard(view.context.getString(R.string.title), presenter.manga.title)
        }

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.manga_info, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_open_in_browser -> openInBrowser()
            R.id.action_open_in_web_view -> openInWebView()
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

        //update full title TextView.
        manga_full_title.text = if (manga.title.isBlank()) {
            view.context.getString(R.string.unknown)
        } else {
            manga.title
        }

        // Update artist TextView.
        manga_artist.text = if (manga.artist.isNullOrBlank()) {
            view.context.getString(R.string.unknown)
        } else {
            manga.artist
        }

        // Update author TextView.
        manga_author.text = if (manga.author.isNullOrBlank()) {
            view.context.getString(R.string.unknown)
        } else {
            manga.author
        }

        // If manga source is known update source TextView.
        manga_source.text = if (source == null) {
            view.context.getString(R.string.unknown)
        } else {
            source.toString()
        }

        // Update genres list
        if (manga.genre.isNullOrBlank().not()) {
            manga_genres_tags.setTags(manga.genre?.split(", "))
        }

        // Update description TextView.
        manga_summary.text = if (manga.description.isNullOrBlank()) {
            view.context.getString(R.string.unknown)
        } else {
            manga.description
        }

        // Update status TextView.
        manga_status.setText(when (manga.status) {
            SManga.ONGOING -> R.string.ongoing
            SManga.COMPLETED -> R.string.completed
            SManga.LICENSED -> R.string.licensed
            else -> R.string.unknown
        })

        // Set the favorite drawable to the correct one.
        setFavoriteDrawable(manga.favorite)

        // Set cover if it wasn't already.
        if (manga_cover.drawable == null && !manga.thumbnail_url.isNullOrEmpty()) {
            GlideApp.with(view.context)
                    .load(manga)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .centerCrop()
                    .into(manga_cover)

            if (backdrop != null) {
                GlideApp.with(view.context)
                        .load(manga)
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .centerCrop()
                        .into(backdrop)
            }
        }
    }

    override fun onDestroyView(view: View) {
        manga_genres_tags.setOnTagClickListener(null)
        super.onDestroyView(view)
    }

    /**
     * Update chapter count TextView.
     *
     * @param count number of chapters.
     */
    fun setChapterCount(count: Float) {
        if (count > 0f) {
            manga_chapters?.text = DecimalFormat("#.#").format(count)
        } else {
            manga_chapters?.text = resources?.getString(R.string.unknown)
        }
    }

    fun setLastUpdateDate(date: Date) {
        if (date.time != 0L) {
            manga_last_update?.text = DateFormat.getDateInstance(DateFormat.SHORT).format(date)
        } else {
            manga_last_update?.text = resources?.getString(R.string.unknown)
        }
    }

    /**
     * Toggles the favorite status and asks for confirmation to delete downloaded chapters.
     */
    private fun toggleFavorite() {
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
    private fun openInBrowser() {
        val context = view?.context ?: return
        val source = presenter.source as? HttpSource ?: return

        context.openInBrowser(source.mangaDetailsRequest(presenter.manga).url().toString())
    }

    private fun openInWebView() {
        val source = presenter.source as? HttpSource ?: return

        val url = try {
            source.mangaDetailsRequest(presenter.manga).url().toString()
        } catch (e: Exception) {
            return
        }

        parentController?.router?.pushController(MangaWebViewController(source.id, url)
            .withFadeTransaction())
    }

    /**
     * Called to run Intent with [Intent.ACTION_SEND], which show share dialog.
     */
    private fun shareManga() {
        val context = view?.context ?: return

        val source = presenter.source as? HttpSource ?: return
        try {
            val url = source.mangaDetailsRequest(presenter.manga).url().toString()
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, url)
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
        fab_favorite?.setImageResource(if (isFavorite)
            R.drawable.ic_bookmark_white_24dp
        else
            R.drawable.ic_add_to_library_24dp)
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
    fun onFetchMangaError(error: Throwable) {
        setRefreshing(false)
        activity?.toast(error.message)
    }

    /**
     * Set swipe refresh status.
     *
     * @param value whether it should be refreshing or not.
     */
    private fun setRefreshing(value: Boolean) {
        swipe_refresh?.isRefreshing = value
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
            when {
                defaultCategory != null -> presenter.moveMangaToCategory(manga, defaultCategory)
                categories.size <= 1 -> // default or the one from the user
                    presenter.moveMangaToCategory(manga, categories.firstOrNull())
                else -> {
                    val ids = presenter.getMangaCategoryIds(manga)
                    val preselected = ids.mapNotNull { id ->
                        categories.indexOfFirst { it.id == id }.takeIf { it != -1 }
                    }.toTypedArray()

                    ChangeMangaCategoriesDialog(this, listOf(manga), categories, preselected)
                            .showDialog(router)
                }
            }
            activity?.toast(activity?.getString(R.string.manga_added_library))
        } else {
            activity?.toast(activity?.getString(R.string.manga_removed_library))
        }
    }

    /**
     * Called when the fab is long clicked.
     */
    private fun onFabLongClick() {
        val manga = presenter.manga
        if (!manga.favorite) {
            toggleFavorite()
            activity?.toast(activity?.getString(R.string.manga_added_library))
        }
        val categories = presenter.getCategories()
        if (categories.size <= 1) {
            // default or the one from the user then just add to favorite.
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

    override fun updateCategoriesForMangas(mangas: List<Manga>, categories: List<Category>) {
        val manga = mangas.firstOrNull() ?: return
        presenter.moveMangaToCategories(manga, categories)
    }

    /**
     * Add a shortcut of the manga to the home screen
     */
    private fun addToHomeScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // TODO are transformations really unsupported or is it just the Pixel Launcher?
            createShortcutForShape()
        } else {
            ChooseShapeDialog(this).showDialog(router)
        }
    }

    /**
     * Dialog to choose a shape for the icon.
     */
    private class ChooseShapeDialog(bundle: Bundle? = null) : DialogController(bundle) {

        constructor(target: MangaInfoController) : this() {
            targetController = target
        }

        override fun onCreateDialog(savedViewState: Bundle?): Dialog {
            val modes = intArrayOf(R.string.circular_icon,
                    R.string.rounded_icon,
                    R.string.square_icon,
                    R.string.star_icon)

            return MaterialDialog.Builder(activity!!)
                    .title(R.string.icon_shape)
                    .negativeText(android.R.string.cancel)
                    .items(modes.map { activity?.getString(it) })
                    .itemsCallback { _, _, i, _ ->
                        (targetController as? MangaInfoController)?.createShortcutForShape(i)
                    }
                    .build()
        }
    }

    /**
     * Retrieves the bitmap of the shortcut with the requested shape and calls [createShortcut] when
     * the resource is available.
     *
     * @param i The shape index to apply. Defaults to circle crop transformation.
     */
    private fun createShortcutForShape(i: Int = 0) {
        if (activity == null) return
        GlideApp.with(activity!!)
                .asBitmap()
                .load(presenter.manga)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .apply {
                    when (i) {
                        0 -> circleCrop()
                        1 -> transform(RoundedCorners(5))
                        2 -> transform(CropSquareTransformation())
                        3 -> centerCrop().transform(MaskTransformation(R.drawable.mask_star))
                    }
                }
                .into(object : SimpleTarget<Bitmap>(96, 96) {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        createShortcut(resource)
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        activity?.toast(R.string.icon_creation_fail)
                    }
                })
    }

    /**
     * Copies a string to clipboard
     *
     * @param label Label to show to the user describing the content
     * @param content the actual text to copy to the board
     */
    private fun copyToClipboard(label: String, content: String) {
        if (content.isBlank()) return

        val activity = activity ?: return
        val view = view ?: return

        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.primaryClip = ClipData.newPlainText(label, content)

        activity.toast(view.context.getString(R.string.copied_to_clipboard, content.truncateCenter(20)),
                Toast.LENGTH_SHORT)
    }

    /**
     * Perform a global search using the provided query.
     *
     * @param query the search query to pass to the search controller
     */
    fun performGlobalSearch(query: String) {
        val router = parentController?.router ?: return
        router.pushController(CatalogueSearchController(query).withFadeTransaction())
    }

    /**
     * Create shortcut using ShortcutManager.
     *
     * @param icon The image of the shortcut.
     */
    private fun createShortcut(icon: Bitmap) {
        val activity = activity ?: return
        val mangaControllerArgs = parentController?.args ?: return

        // Create the shortcut intent.
        val shortcutIntent = activity.intent
                .setAction(MainActivity.SHORTCUT_MANGA)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(MangaController.MANGA_EXTRA,
                        mangaControllerArgs.getLong(MangaController.MANGA_EXTRA))

        // Check if shortcut placement is supported
        if (ShortcutManagerCompat.isRequestPinShortcutSupported(activity)) {
            val shortcutId = "manga-shortcut-${presenter.manga.title}-${presenter.source.name}"

            // Create shortcut info
            val shortcutInfo = ShortcutInfoCompat.Builder(activity, shortcutId)
                    .setShortLabel(presenter.manga.title)
                    .setIcon(IconCompat.createWithBitmap(icon))
                    .setIntent(shortcutIntent)
                    .build()

            val successCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Create the CallbackIntent.
                val intent = ShortcutManagerCompat.createShortcutResultIntent(activity, shortcutInfo)

                // Configure the intent so that the broadcast receiver gets the callback successfully.
                PendingIntent.getBroadcast(activity, 0, intent, 0)
            } else {
                NotificationReceiver.shortcutCreatedBroadcast(activity)
            }

            // Request shortcut.
            ShortcutManagerCompat.requestPinShortcut(activity, shortcutInfo,
                    successCallback.intentSender)
        }
    }

}
