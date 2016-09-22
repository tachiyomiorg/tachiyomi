package eu.kanade.tachiyomi.ui.manga.info

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.view.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.source.Source
import eu.kanade.tachiyomi.data.source.online.OnlineSource
import eu.kanade.tachiyomi.ui.base.fragment.BaseRxFragment
import eu.kanade.tachiyomi.util.getResourceColor
import eu.kanade.tachiyomi.util.toast
import kotlinx.android.synthetic.main.fragment_manga_info.*
import nucleus.factory.RequiresPresenter

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
