package eu.kanade.tachiyomi.ui.manga.info

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.design.widget.Snackbar
import android.view.*
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.BitmapRequestBuilder
import com.bumptech.glide.BitmapTypeRequest
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.source.Source
import eu.kanade.tachiyomi.data.source.online.OnlineSource
import eu.kanade.tachiyomi.ui.base.fragment.BaseRxFragment
import eu.kanade.tachiyomi.util.getResourceColor
import eu.kanade.tachiyomi.util.toast
import jp.wasabeef.glide.transformations.CropCircleTransformation
import jp.wasabeef.glide.transformations.CropSquareTransformation
import jp.wasabeef.glide.transformations.RoundedCornersTransformation
import kotlinx.android.synthetic.main.fragment_manga_info.*
import kotlinx.android.synthetic.main.item_download.*
import nucleus.factory.RequiresPresenter
import kotlin.concurrent.thread

/**
 * Fragment that shows manga information.
 * Uses R.layout.fragment_manga_info.
 * UI related actions should be called from here.
 */
@RequiresPresenter(MangaInfoPresenter::class)
class MangaInfoFragment : BaseRxFragment<MangaInfoPresenter>() {

    companion object {
        /**
         * Create new instance of MangaInfoFragment.
         *
         * @return MangaInfoFragment.
         */
        fun newInstance(): MangaInfoFragment {
            return MangaInfoFragment()
        }
    }

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_manga_info, container, false)
    }

    override fun onViewCreated(view: View?, savedState: Bundle?) {
        // Set onclickListener to toggle favorite when FAB clicked.
        fab_favorite.setOnClickListener { presenter.toggleFavorite() }

        // Set SwipeRefresh to refresh manga data.
        swipe_refresh.setOnRefreshListener { fetchMangaFromSource() }
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
            Manga.ONGOING -> R.string.ongoing
            Manga.COMPLETED -> R.string.completed
            Manga.LICENSED -> R.string.licensed
            else -> R.string.unknown
        })

        // Update description TextView.
        manga_summary.text = manga.description

        // Set the favorite drawable to the correct one.
        setFavoriteDrawable(manga.favorite)

        // Set cover if it wasn't already.
        if (manga_cover.drawable == null && !manga.thumbnail_url.isNullOrEmpty()) {
            Glide.with(this)
                    .load(manga)
                    .diskCacheStrategy(DiskCacheStrategy.RESULT)
                    .centerCrop()
                    .into(manga_cover)

            Glide.with(this)
                    .load(manga)
                    .diskCacheStrategy(DiskCacheStrategy.RESULT)
                    .centerCrop()
                    .into(backdrop)
        }
    }

    /**
     * Update chapter count TextView.
     *
     * @param count number of chapters.
     */
    fun setChapterCount(count: Int) {
        manga_chapters.text = count.toString()
    }

    /**
     * Open the manga in browser.
     */
    fun openInBrowser() {
        val source = presenter.source as? OnlineSource ?: return
        try {
            val url = Uri.parse(source.baseUrl + presenter.manga.url)
            val intent = CustomTabsIntent.Builder()
                    .setToolbarColor(context.theme.getResourceColor(R.attr.colorPrimary))
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
        val source = presenter.source as? OnlineSource ?: return
        try {
            val url = source.mangaDetailsRequest(presenter.manga).url().toString()
            val sharingIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_SUBJECT, presenter.manga.title)
                putExtra(android.content.Intent.EXTRA_TEXT, resources.getString(R.string.share_text, presenter.manga.title, url))
            }
            startActivity(Intent.createChooser(sharingIntent, resources.getText(R.string.share_subject)))
        } catch (e: Exception) {
            context.toast(e.message)
        }
    }

    /**
     * Add the manga to the home screen
     */
    fun addToHomeScreen() {
        val shortcutIntent = activity.intent
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val addIntent = Intent()
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
                .action = "com.android.launcher.action.INSTALL_SHORTCUT"

        val modes = intArrayOf(R.string.tachiyomi_icon,
                R.string.circular_manga_icon,
                R.string.rounded_manga_icon,
                R.string.square_manga_icon,
                R.string.custom_icon)

        //Set shortcut title
        var title = presenter.manga.title
        MaterialDialog.Builder(activity)
                .title(R.string.shortcut_title)
                .input("", title, { md, text -> title = text.toString() })
                .negativeText(android.R.string.cancel)
                .onNegative { materialDialog, dialogAction -> materialDialog.cancel() }
                .onPositive { materialDialog, dialogAction ->
                    //Glide utility methods
                    fun mangaBitmap(): BitmapTypeRequest<Manga> {
                        return Glide.with(context).load(presenter.manga).asBitmap()
                    }
                    fun BitmapRequestBuilder<Manga, Bitmap>.toIcon(): Bitmap {
                        return this.into(96, 96).get()
                    }

                    //Set shortcut icon
                    MaterialDialog.Builder(activity)
                            .title(R.string.icon_type)
                            .negativeText(android.R.string.cancel)
                            .items(modes.map { getString(it) })
                            .itemsCallback { dialog, view, i, charSequence ->
                                thread {
                                    // i = 0: Tachiyomi icon
                                    // i = 1: Circular manga icon
                                    // i = 2: Rounded manga icon
                                    // i = 3: Square manga icon
                                    // i = 4: TODO Custom icon
                                    val icon = when (i) {
                                        0 -> BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
                                        1 -> mangaBitmap().transform(CropCircleTransformation(context)).toIcon()
                                        2 -> mangaBitmap().transform(RoundedCornersTransformation(context, 5, 0)).toIcon()
                                        3 -> mangaBitmap().transform(CropSquareTransformation(context)).toIcon()
                                        4 -> null //TODO
                                        else -> null
                                    }

                                    if (icon != null) {
                                        //Send shortcut intent
                                        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, title)
                                                .putExtra(Intent.EXTRA_SHORTCUT_ICON, icon)
                                        context.sendBroadcast(addIntent)
                                        //Go to launcher to show this shiny new shortcut!
                                        val startMain = Intent(Intent.ACTION_MAIN)
                                        startMain.addCategory(Intent.CATEGORY_HOME)
                                                .flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        activity.runOnUiThread {
                                            startActivity(startMain)
                                        }
                                    }
                                }
                            }.show()
                }.show()
    }

    /**
     * Update FAB with correct drawable.
     *
     * @param isFavorite determines if manga is favorite or not.
     */
    private fun setFavoriteDrawable(isFavorite: Boolean) {
        // Set the Favorite drawable to the correct one.
        // Border drawable if false, filled drawable if true.
        fab_favorite.setImageResource(if (isFavorite)
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
        swipe_refresh.isRefreshing = value
    }

}
