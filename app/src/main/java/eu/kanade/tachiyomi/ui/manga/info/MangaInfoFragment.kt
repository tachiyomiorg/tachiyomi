package eu.kanade.tachiyomi.ui.manga.info

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.annotation.Nullable
import android.support.customtabs.CustomTabsIntent
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaSync
import eu.kanade.tachiyomi.data.source.Source
import eu.kanade.tachiyomi.data.source.online.OnlineSource
import eu.kanade.tachiyomi.ui.base.fragment.BaseRxFragment
import eu.kanade.tachiyomi.util.getResourceColor
import eu.kanade.tachiyomi.util.setDrawableTop
import eu.kanade.tachiyomi.util.setPositionByValue
import eu.kanade.tachiyomi.util.toast
import eu.kanade.tachiyomi.widget.IgnoreFirstSpinnerListener
import kotlinx.android.synthetic.main.dialog_manga_sync_chapters.view.*
import kotlinx.android.synthetic.main.dialog_manga_sync_edit.view.*
import kotlinx.android.synthetic.main.dialog_manga_sync_score.view.*
import kotlinx.android.synthetic.main.fragment_manga_info.*
import kotlinx.android.synthetic.main.fragment_manga_info.view.*
import nucleus.factory.RequiresPresenter

/**
 * Fragment that shows manga information and sync status.
 * Uses [R.layout.fragment_manga_info].
 * UI related actions should be called from here.
 * Observable updates should be called from [MangaInfoPresenter]
 */
@RequiresPresenter(MangaInfoPresenter::class)
class MangaInfoFragment : BaseRxFragment<MangaInfoPresenter>() {
    companion object {
        /**
         * Tag used for [MangaSyncSearchDialogFragment]
         */
        const val SEARCH_FRAGMENT_TAG = "sync_search"

        /**
         * Create new [MangaInfoFragment].
         */
        @JvmStatic
        fun newInstance(): MangaInfoFragment {
            return MangaInfoFragment()
        }
    }

    /**
     * Instance of [MangaSyncSearchDialogFragment]
     */
    private var search_dialog: MangaSyncSearchDialogFragment? = null

    /**
     * Instance of [MaterialDialog]
     */
    private var last_read_dialog: MaterialDialog? = null

    /**
     * Instance of [MaterialDialog]
     */
    private var score_dialog: MaterialDialog? = null

    /**
     * Adapter containing the manga sync.
     */
    lateinit var adapter: MangaSyncAdapter
        private set

    /**
     * View used to show sync options.
     * Inflates from [R.layout.dialog_manga_sync_edit]
     */
    lateinit var syncView: View
        private set

    /**
     * Boolean used to check if the sync info view information is initialized.
     */
    private var isSyncInfoInitialized: Boolean = false

    /**
     * Called to have the fragment instantiate its user interface view.
     * This will be called between onCreate(Bundle) and onActivityCreated(Bundle).
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return The View for the fragment's UI, or null.
     */
    override fun onCreateView(inflater: LayoutInflater, @Nullable container: ViewGroup?, @Nullable savedInstanceState: Bundle?): View? {
        // Inflate syncView.
        syncView = inflater.inflate(R.layout.dialog_manga_sync_edit, container, false)
        // Inflate manga options view.
        return inflater.inflate(R.layout.fragment_manga_info, container, false)
    }

    /**
     * Called immediately after onCreateView(LayoutInflater, ViewGroup, Bundle) has returned,
     * but before any saved state has been restored in to the view.
     * This gives subclasses a chance to initialize themselves once they know their view
     * hierarchy has been completely created.
     * @param view he View returned by onCreateView(LayoutInflater, ViewGroup, Bundle).
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     */
    override fun onViewCreated(view: View, @Nullable savedInstanceState: Bundle?) {
        // Set the correct tint of the drawables
        setDrawables()

        // Set OnClickListener to remove or add manga to library
        btn_add_to_library.setOnClickListener() {
            //Create prompt telling user manga sync will be deleted
            if (presenter.manga.favorite && presenter.isLogged()) {
                MaterialDialog.Builder(context)
                        .title(R.string.action_remove)
                        .content(R.string.remove_from_library_warning)
                        .positiveText(R.string.action_remove)
                        .negativeText(android.R.string.cancel)
                        .onPositive { materialDialog, dialogAction ->
                            presenter.addOrRemoveFromLibrary()
                        }
                        .onNegative { materialDialog, dialogAction ->
                            materialDialog.dismiss()
                        }
                        .show()
            } else
                presenter.addOrRemoveFromLibrary()
        }

        // Set OnClickListener to share manga
        btn_share.setOnClickListener() {
            shareManga()
        }

        // Set OnclickListener to open manga in browser
        btn_open_in_browser.setOnClickListener() {
            openInBrowser()
        }

        // Set OnRefreshListener to refresh manga data.
        swipe_refresh.setOnRefreshListener {
            fetchMangaFromSource()
        }
    }

    /**
     * Called when [MangaInfoPresenter] has fetched [Manga] items
     * Used to check if manga is initialized
     * True: Update view with manga information
     * False: Fetch manga information
     * @param manga Manga object containing information about manga.
     */
    fun onNextManga(manga: Manga, source: Source) {
        if (manga.initialized) {
            // Update manga info.
            setMangaInfo(manga, source)
            // Update sync info
            setSyncInfo()
        } else {
            // Initialize manga.
            fetchMangaFromSource()
        }
    }

    /**
     * Called when [MangaInfoPresenter] has fetched [MangaSync] items
     * When fetched update view with Sync information
     * @param mangaSync [MangaSync] object containing information about [MangaSync].
     */
    fun onNextMangaSync(mangaSync: List<MangaSync>) {
        if (!isSyncInfoInitialized)
            initializeSyncInfo()

        adapter.setItems(mangaSync)

        score_layout.visibility = View.GONE

        mangaSync.forEach {
            if (isDefaultService(it)) {
                setDefaultMangaSyncInfo(it)
                return
            }
        }
    }

    /**
     * Called to check if [MangaSync] object is bind to the default Manga service
     * @return result of check if bind to default manga service
     */
    fun isDefaultService(mangaSync: MangaSync): Boolean {
        return presenter.isDefaultService(mangaSync)
    }

    /**
     * Called to populate View with information
     * @param manga Object containing attributes of [Manga]
     * @param source Object containing attributes of [Source]
     */
    private fun setMangaInfo(manga: Manga, source: Source?) {
        // Set TextView values
        manga_artist.text = if (manga.artist.isNullOrBlank()) "Unknown" else manga.artist
        manga_author.text = if (manga.author.isNullOrBlank()) "Unknown" else manga.author
        manga_genres.text = manga.genre
        // Update status TextView.
        manga_status.setText(when (manga.status) {
            Manga.ONGOING -> R.string.ongoing
            Manga.COMPLETED -> R.string.completed
            Manga.LICENSED -> R.string.licensed
            else -> R.string.unknown
        })
        manga_summary.text = manga.description

        // If manga source is known update source TextView.
        source.let { manga_source.text = it.toString() }

        // Set the bookmark drawable to the correct one.
        setBookmarkDrawable(manga.favorite)

        // Set cover if isn't initialized yet
        if (manga_cover.drawable == null && !manga.thumbnail_url.isNullOrEmpty()) {
            Glide.with(this)
                    .load(manga)
                    .diskCacheStrategy(DiskCacheStrategy.RESULT)
                    .fitCenter()
                    .into(manga_cover)

            Glide.with(this)
                    .load(manga)
                    .diskCacheStrategy(DiskCacheStrategy.RESULT)
                    .centerCrop()
                    .into(manga_backdrop)
        }
    }

    /**
     * Called to set sync info when [onNextManga]
     */
    private fun setSyncInfo() {
        if (!presenter.manga.favorite || !presenter.isLogged()) {
            btn_sync_options.visibility = View.GONE
        } else {
            initializeSyncInfo()

            btn_sync_options.visibility = View.VISIBLE
        }
    }

    /**
     * Called to initialize the onClick Listeners and adapter of the syncView
     */
    private fun initializeSyncInfo() {
        if (isSyncInfoInitialized)
            return

        // Set OnclickListener to open sync option view
        btn_sync_options.setOnClickListener {
            MaterialDialog.Builder(context)
                    .title(R.string.sync_options)
                    .customView(syncView, true)
                    .negativeText(R.string.action_close)
                    .onNegative { materialDialog, dialogAction ->
                        materialDialog.dismiss()
                    }.show()
        }

        //Initialize last read dialog
        last_read_dialog = MaterialDialog.Builder(activity)
                .title(R.string.chapters)
                .customView(R.layout.dialog_manga_sync_chapters, false)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .onPositive { materialDialog, dialogAction ->
                    materialDialog.customView?.let {
                        it.chapters_picker.clearFocus()
                        presenter.setLastChapterRead(it.chapters_picker.value)
                    }
                }.build()

        // Don't allow to go from 0 to 9999
        last_read_dialog?.customView?.chapters_picker?.wrapSelectorWheel = false

        //Initialize score dialog
        score_dialog = MaterialDialog.Builder(activity)
                .title(R.string.score)
                .customView(R.layout.dialog_manga_sync_score, false)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .onPositive { materialDialog, dialogAction ->
                    materialDialog.customView?.let {
                        it.score_picker.clearFocus()
                        //After user selects score update sync
                        presenter.setScore(it.score_picker.value)
                    }
                }.build()

        // Set OnclickListener to show score dialog
        btn_rate.setOnClickListener() {
            score_dialog?.show()
        }
        // Initialize adapter
        syncView.list_bind_items.layoutManager = LinearLayoutManager(activity)
        adapter = MangaSyncAdapter(this)
        syncView.list_bind_items.setHasFixedSize(true)
        syncView.list_bind_items.adapter = adapter

        syncView.btn_last_read_edit.setOnClickListener() {
            last_read_dialog?.show()
        }

        val spinnerAdapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_item)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAdapter.addAll(presenter.statusMap.values)
        syncView.spinner_status.adapter = spinnerAdapter

        // Set onItemSelectedListener to update status when new status selected by user
        syncView.spinner_status.onItemSelectedListener = IgnoreFirstSpinnerListener { parent, position ->
            presenter.setStatus(parent?.getItemAtPosition(position).toString())
        }
        isSyncInfoInitialized = true
    }


    /**
     * Called to run Intent with [Intent.ACTION_SEND]
     */
    private fun shareManga() {
        val source = presenter.source as? OnlineSource ?: return
        try {
            val url = source.mangaDetailsRequest(presenter.manga).url().toString()
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.type = "text/plain"
            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, presenter.manga.title)
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, url)
            startActivity(Intent.createChooser(sharingIntent, "Share.."))
        } catch (e: Exception) {
            context.toast(e.message)
        }
    }

    /**
     * Called to open the [Manga] in browser.
     */
    fun openInBrowser() {
        val source = presenter.source as? OnlineSource ?: return
        try {
            val url = Uri.parse(source.mangaDetailsRequest(presenter.manga).url().toString())
            val intent = CustomTabsIntent.Builder()
                    .setToolbarColor(context.theme.getResourceColor(R.attr.colorPrimary))
                    .build()
            intent.launchUrl(activity, url)
        } catch (e: Exception) {
            context.toast(e.message)
        }
    }


    /**
     * Called to set last_read and status from default [MangaSync]
     * @param mangaSync [MangaSync] object containing information about [MangaSync].
     */
    fun setDefaultMangaSyncInfo(mangaSync: MangaSync) {
        setSpinnerValue(mangaSync)

        syncView.txt_last_read_label.text = mangaSync.last_chapter_read.toString()

        last_read_dialog?.customView?.chapters_picker?.value = mangaSync.last_chapter_read

        score_dialog?.customView?.score_picker?.value = presenter.getScore(mangaSync).toInt()

        score.text = presenter.getRemoteScore(mangaSync).toString()

        btn_rate.text = "Rate(" + presenter.getScore(mangaSync).toInt().toString() + ")"

        score_layout.visibility = View.VISIBLE

        setEditViewVisibility(true)
    }

    /**
     * Changes the visibility of the sync option.
     */
    fun setEditViewVisibility(visible: Boolean) {
        syncView.txt_status_title.visibility = if (visible) View.VISIBLE else View.GONE
        syncView.spinner_status.visibility = if (visible) View.VISIBLE else View.GONE
        syncView.txt_last_read_title.visibility = if (visible) View.VISIBLE else View.GONE
        syncView.txt_last_read_label.visibility = if (visible) View.VISIBLE else View.GONE
        syncView.btn_last_read_edit.visibility = if (visible) View.VISIBLE else View.GONE
    }

    /**
     * Called to show [MangaSyncSearchDialogFragment].
     * Creates dialog when not yet initialized
     * @param syncId id of the [MangaSync] service
     */
    fun showSearchDialog(syncId: Int) {
        if (search_dialog == null)
            search_dialog = MangaSyncSearchDialogFragment.newInstance(syncId)
        else
            search_dialog?.search(presenter.manga.title, syncId, true)

        search_dialog?.show(childFragmentManager, SEARCH_FRAGMENT_TAG)
    }

    /**
     * Called to start fetching manga information from source.
     */
    private fun fetchMangaFromSource() {
        swipe_refresh.isRefreshing = true
        // Call presenter and start fetching manga information
        presenter.fetchMangaFromSource()
    }

    /**
     * Update swipe refresh to stop showing refresh in progress spinner.
     * If manga has bind manga start fetching [MangaSync] updates from service.
     */
    fun onFetchMangaDone() {
        if (presenter.hasBind())
            presenter.fetchSyncInfoFromSource()
        else
            swipe_refresh.isRefreshing = false
    }

    /**
     * Update swipe refresh to stop showing refresh in progress spinner.
     */
    fun onRefreshSyncDone() {
        swipe_refresh.isRefreshing = false
    }

    /**
     * Update swipe refresh to start showing refresh in progress spinner.
     */
    fun onFetchMangaError(error: Throwable) {
        swipe_refresh.isRefreshing = false
        context.toast(error.message)
    }

    /**
     * Called to set search results in [MangaSyncSearchDialogFragment]
     * @param results list containing [MangaSync] results from search query
     */
    fun setSearchResults(results: List<MangaSync>) {
        search_dialog?.onSearchResults(results)
    }

    /**
     * Called when error is thrown while fetching search results
     * @param error error thrown while fetching
     */
    fun setSearchResultsError(error: Throwable) {
        context.toast(error.message)
        search_dialog?.onSearchResultsError()
    }

    /**
     * Update the chapter TextView.
     * @param count number of Chapters
     */
    fun setChapterCount(count: Int) {
        manga_chapters.text = count.toString() + " Chapters"
    }

    /**
     * Update the status spinner
     * @param mangaSync [MangaSync] object containing information about [MangaSync].
     */
    fun setSpinnerValue(mangaSync: MangaSync) {
        // Update selection
        syncView.spinner_status.setPositionByValue(presenter.getStatus(mangaSync))
    }

    /**
     * Update [btn_add_to_library] with correct drawable.
     * @param isAdded determines if manga is added or removed.
     */
    private fun setBookmarkDrawable(isAdded: Boolean) {
        btn_add_to_library.setDrawableTop(if (isAdded)
            R.drawable.ic_bookmark_white_24dp
        else
            R.drawable.ic_bookmark_border_white_24dp,
                context.theme.getResourceColor(android.R.attr.textColorPrimary))

        btn_add_to_library.text = if (isAdded) "Remove from library" else "Add to library"
    }

    /**
     * Called to set the correct tint of the drawables
     */
    private fun setDrawables() {
        btn_sync_options.setDrawableTop(R.drawable.ic_sync_white_24dp,
                context.theme.getResourceColor(android.R.attr.textColorPrimary))

        btn_share.setDrawableTop(R.drawable.ic_share_white_24dp,
                context.theme.getResourceColor(android.R.attr.textColorPrimary))

        btn_open_in_browser.setDrawableTop(R.drawable.ic_open_in_browser_white_24dp,
                context.theme.getResourceColor(android.R.attr.textColorPrimary))
    }

    /**
     * Called to remove [MangaSync] object from database
     * @param mangaSync [MangaSync] object containing information about [MangaSync].
     */
    fun unbindMangaSync(mangaSync: MangaSync) {
        presenter.unbindMangaSync(mangaSync)
    }
}