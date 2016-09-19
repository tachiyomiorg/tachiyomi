package eu.kanade.tachiyomi.ui.latest_updates

import android.content.res.Configuration
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import com.afollestad.materialdialogs.MaterialDialog
import com.f2prateek.rx.preferences.Preference
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.ui.base.adapter.FlexibleViewHolder
import eu.kanade.tachiyomi.ui.base.fragment.BaseRxFragment
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.manga.MangaActivity
import eu.kanade.tachiyomi.util.getResourceDrawable
import eu.kanade.tachiyomi.util.snack
import eu.kanade.tachiyomi.util.toast
import eu.kanade.tachiyomi.widget.DividerItemDecoration
import eu.kanade.tachiyomi.widget.EndlessScrollListener
import eu.kanade.tachiyomi.widget.IgnoreFirstSpinnerListener
import kotlinx.android.synthetic.main.fragment_latest_updates.*
import kotlinx.android.synthetic.main.fragment_manga_chapters.*
import kotlinx.android.synthetic.main.toolbar.*
import nucleus.factory.RequiresPresenter
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.TimeUnit.MILLISECONDS

/**
 * Fragment that shows the manga from the catalogue.
 * Uses R.layout.fragment_catalogue.
 */
@RequiresPresenter(LatestUpdatesPresenter::class)
class LatestUpdatesFragment : BaseRxFragment<LatestUpdatesPresenter>(), FlexibleViewHolder.OnListItemClickListener {

    /**
     * Spinner shown in the toolbar to change the selected source.
     */
    private lateinit var spinner: Spinner

    /**
     * Adapter containing the list of manga from the catalogue.
     */
    private lateinit var updatesAdapter: LatestUpdatesAdapter

    /**
     * Scroll listener for grid mode. It loads next pages when the end of the list is reached.
     */
    private lateinit var gridScrollListener: EndlessScrollListener

    /**
     * Scroll listener for list mode. It loads next pages when the end of the list is reached.
     */
    private lateinit var listScrollListener: EndlessScrollListener

    /**
     * Selected index of the spinner (selected source).
     */
    private var selectedIndex: Int = 0

    /**
     * Time in milliseconds to wait for input events in the search query before doing network calls.
     */
    private val SEARCH_TIMEOUT = 1000L

    /**
     * Subject to debounce the query.
     */
    private val queryDebouncerSubject = PublishSubject.create<String>()

    /**
     * Subscription of the debouncer subject.
     */
    private var queryDebouncerSubscription: Subscription? = null

    /**
     * Subscription of the number of manga per row.
     */
    private var numColumnsSubscription: Subscription? = null

    /**
     * Search item.
     */
    private var searchItem: MenuItem? = null

    /**
     * Property to get the toolbar from the containing activity.
     */
    private val toolbar: Toolbar
        get() = (activity as MainActivity).toolbar

    companion object {
        /**
         * Creates a new instance of this fragment.
         *
         * @return a new instance of [LatestUpdatesFragment].
         */
        fun newInstance(): LatestUpdatesFragment {
            return LatestUpdatesFragment()
        }
    }

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_latest_updates, container, false)
    }

    override fun onViewCreated(view: View, savedState: Bundle?) {
        // Initialize updatesAdapter, scroll listener and recycler views
        updatesAdapter = LatestUpdatesAdapter(this) //updatesAdapter null? causes WAIT


        val glm = latest_updates_grid.layoutManager as GridLayoutManager
        gridScrollListener = EndlessScrollListener(glm, { requestNextPage() })
        latest_updates_grid.setHasFixedSize(true)
        latest_updates_grid.adapter = updatesAdapter
        latest_updates_grid.addOnScrollListener(gridScrollListener)

        // swipe_refresh.setOnRefreshListener { presenter.restartPager() } TODO

        val llm = LinearLayoutManager(activity)
        listScrollListener = EndlessScrollListener(llm, { requestNextPage() })
        latest_updates_list.setHasFixedSize(true)
        latest_updates_list.adapter = updatesAdapter
        latest_updates_list.layoutManager = llm
        latest_updates_list.addOnScrollListener(listScrollListener)
        latest_updates_list.addItemDecoration(
                DividerItemDecoration(context.theme.getResourceDrawable(R.attr.divider_drawable)))

        if (presenter.isListMode) {
            switcher.showNext()
        }

        numColumnsSubscription = getColumnsPreferenceForCurrentOrientation().asObservable()
                .doOnNext { latest_updates_grid.spanCount = it }
                .skip(1)
                // Set again the updatesAdapter to recalculate the covers height
                .subscribe { latest_updates_grid.adapter = updatesAdapter }

        switcher.inAnimation = AnimationUtils.loadAnimation(activity, android.R.anim.fade_in)
        switcher.outAnimation = AnimationUtils.loadAnimation(activity, android.R.anim.fade_out)

        // Create toolbar spinner
        val themedContext = activity.supportActionBar?.themedContext ?: activity

        val spinnerAdapter = ArrayAdapter(themedContext,
                android.R.layout.simple_spinner_item, presenter.sources)
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_item)

        val onItemSelected = IgnoreFirstSpinnerListener { position ->
            val source = spinnerAdapter.getItem(position)
            if (presenter.isValidSource(source) != 2) {
                spinner.setSelection(selectedIndex)
                if (presenter.isValidSource(source) == 1) {
                    context.toast(R.string.source_requires_login)
                } else {
                    context.toast(R.string.source_unsupported_operation)
                }
            } else if (source != presenter.source) {
                selectedIndex = position
                showProgressBar()
                glm.scrollToPositionWithOffset(0, 0)
                llm.scrollToPositionWithOffset(0, 0)
                presenter.setActiveSource(source)
                activity.invalidateOptionsMenu()
            }
        }

        selectedIndex = presenter.sources.indexOf(presenter.source)

        spinner = Spinner(themedContext).apply {
            adapter = spinnerAdapter
            setSelection(selectedIndex)
            onItemSelectedListener = onItemSelected
        }

        setToolbarTitle("")
        toolbar.addView(spinner)

        showProgressBar()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.catalogue_list, menu)
        val mode = menu.findItem(R.id.action_display_mode)
        menu.findItem(R.id.action_search).isVisible = false
        menu.findItem(R.id.action_set_filter).isVisible = false
        mode.isVisible = true

        // Show next display mode
        mode.apply {
            val icon = if (presenter.isListMode)
                R.drawable.ic_view_module_white_24dp
            else
                R.drawable.ic_view_list_white_24dp
            setIcon(icon)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_display_mode -> swapDisplayMode()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        queryDebouncerSubscription = queryDebouncerSubject.debounce(SEARCH_TIMEOUT, MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {}
    }

    override fun onPause() {
        queryDebouncerSubscription?.unsubscribe()
        super.onPause()
    }

    override fun onDestroyView() {
        numColumnsSubscription?.unsubscribe()
        searchItem?.let {
            if (it.isActionViewExpanded) it.collapseActionView()
        }
        toolbar.removeView(spinner)
        super.onDestroyView()
    }

    /**
     * Requests the next page (if available). Called from scroll listeners when they reach the end.
     */
    private fun requestNextPage() {
        if (presenter.hasNextPage()) {
            showGridProgressBar()
            presenter.requestNext()
        }
    }

    /**
     * Called from the presenter when the network request is received.
     *
     * @param page the current page.
     * @param mangas the list of manga of the page.
     */
    fun onAddPage(page: Int, mangas: List<Manga>) {
        hideProgressBar()
        if (page == 1) {
            updatesAdapter.clear()
            gridScrollListener.resetScroll()
            listScrollListener.resetScroll()
        }
        updatesAdapter.addItems(mangas)
    }

    /**
     * Called from the presenter when the network request fails.
     *
     * @param error the error received.
     */
    fun onAddPageError(error: Throwable) {
        hideProgressBar()
        Timber.e(error)

        latest_updates_view.snack(error.message ?: "", Snackbar.LENGTH_INDEFINITE) {
            setAction(R.string.action_retry) {
                showProgressBar()
                presenter.requestNext()
            }
        }
    }

    /**
     * Called from the presenter when a manga is initialized.
     *
     * @param manga the manga initialized
     */
    fun onMangaInitialized(manga: Manga) {
        getHolder(manga)?.setImage(manga)
    }

    /**
     * Swaps the current display mode.
     */
    fun swapDisplayMode() {
        presenter.swapDisplayMode()
        val isListMode = presenter.isListMode
        activity.invalidateOptionsMenu()
        switcher.showNext()
        if (!isListMode) {
            // Initialize mangas if going to grid view
            presenter.initializeMangas(updatesAdapter.items)
        }
    }

    /**
     * Returns a preference for the number of manga per row based on the current orientation.
     *
     * @return the preference.
     */
    fun getColumnsPreferenceForCurrentOrientation(): Preference<Int> {
        return if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
            presenter.prefs.portraitColumns()
        else
            presenter.prefs.landscapeColumns()
    }

    /**
     * Returns the view holder for the given manga.
     *
     * @param manga the manga to find.
     * @return the holder of the manga or null if it's not bound.
     */
    private fun getHolder(manga: Manga): LatestUpdatesGridHolder? {
        return latest_updates_grid.findViewHolderForItemId(manga.id!!) as? LatestUpdatesGridHolder
    }

    /**
     * Shows the progress bar.
     */
    private fun showProgressBar() {
        progress.visibility = ProgressBar.VISIBLE
    }

    /**
     * Shows the progress bar at the end of the screen.
     */
    private fun showGridProgressBar() {
        progress_grid.visibility = ProgressBar.VISIBLE
    }

    /**
     * Hides active progress bars.
     */
    private fun hideProgressBar() {
        progress.visibility = ProgressBar.GONE
        progress_grid.visibility = ProgressBar.GONE
    }

    /**
     * Called when a manga is clicked.
     *
     * @param position the position of the element clicked.
     * @return true if the item should be selected, false otherwise.
     */
    override fun onListItemClick(position: Int): Boolean {
        val item = updatesAdapter.getItem(position) ?: return false

        val intent = MangaActivity.newIntent(activity, item, true)
        startActivity(intent)
        return false
    }

    /**
     * Called when a manga is long clicked.
     *
     * @param position the position of the element clicked.
     */
    override fun onListItemLongClick(position: Int) {
        val manga = updatesAdapter.getItem(position) ?: return

        val textRes = if (manga.favorite) R.string.remove_from_library else R.string.add_to_library

        MaterialDialog.Builder(activity)
                .items(getString(textRes))
                .itemsCallback { dialog, itemView, which, text ->
                    when (which) {
                        0 -> {
                            presenter.changeMangaFavorite(manga)
                            updatesAdapter.notifyItemChanged(position)
                        }
                    }
                }.show()
    }

}
