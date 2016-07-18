package eu.kanade.tachiyomi.ui.library

import android.content.res.Configuration
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import com.f2prateek.rx.preferences.Preference
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.adapter.FlexibleViewHolder
import eu.kanade.tachiyomi.ui.base.fragment.BaseFragment
import eu.kanade.tachiyomi.ui.manga.MangaActivity
import eu.kanade.tachiyomi.util.toast
import kotlinx.android.synthetic.main.fragment_catalogue.*
import kotlinx.android.synthetic.main.fragment_library.*
import kotlinx.android.synthetic.main.fragment_library_category.*
import rx.Subscription
import uy.kohesive.injekt.injectLazy

/**
 * Fragment containing the library manga for a certain category.
 * Uses R.layout.fragment_library_category.
 */
class LibraryCategoryFragment : BaseFragment(), FlexibleViewHolder.OnListItemClickListener {

    /**
     * Preferences.
     */
    val preferences: PreferencesHelper by injectLazy()

    /**
     * Adapter to hold the manga in this category.
     */
    lateinit var adapter: LibraryCategoryAdapter
        private set

    /**
     * Position in the adapter from [LibraryAdapter].
     */
    private var position: Int = 0

    /**
     * Subscription for the library manga.
     */
    private var libraryMangaSubscription: Subscription? = null

    /**
     * Subscription of the number of manga per row.
     */
    private var numColumnsSubscription: Subscription? = null

    /**
     * subscription to view toggle
     */
    private var toggleViewSubscription: Subscription? = null

    /**
     * Subscription of the library search.
     */
    private var searchSubscription: Subscription? = null

    /**
     * display mode
     */
    private var displayAsList: Boolean = false;

    companion object {
        /**
         * Key to save and restore [position] from a [Bundle].
         */
        const val POSITION_KEY = "position_key"

        /**
         * Creates a new instance of this class.
         *
         * @param position the position in the adapter from [LibraryAdapter].
         * @return a new instance of [LibraryCategoryFragment].
         */
        fun newInstance(position: Int): LibraryCategoryFragment {
            val fragment = LibraryCategoryFragment()
            fragment.position = position

            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_library_category, container, false)


    }

    override fun onViewCreated(view: View, savedState: Bundle?) {
        adapter = LibraryCategoryAdapter(this)

        //set up grid
        recycler.setHasFixedSize(true)
        recycler.adapter = adapter

        //set up list
        library_list.setHasFixedSize(true)
        library_list.adapter = adapter
        library_list.layoutManager = LinearLayoutManager(activity)

        if (libraryFragment.actionMode != null) {
            setSelectionMode(FlexibleAdapter.MODE_MULTI)
        }

        numColumnsSubscription = getColumnsPreferenceForCurrentOrientation().asObservable()
                .doOnNext { recycler.spanCount = it }
                .skip(1)
                // Set again the adapter to recalculate the covers height
                .subscribe { recycler.adapter = adapter }

        searchSubscription = libraryPresenter.searchSubject.subscribe { text ->
            adapter.searchText = text
            adapter.updateDataSet()
        }

        toggleViewSubscription = preferences.libraryAsList().asObservable().subscribe {onViewModeChange(it)}

        if(libraryPresenter.displayAsList != displayAsList) {
            library_switcher.showNext()
            displayAsList = libraryPresenter.displayAsList
        }


        library_switcher.inAnimation = AnimationUtils.loadAnimation(activity, android.R.anim.fade_in)
        library_switcher.outAnimation = AnimationUtils.loadAnimation(activity, android.R.anim.fade_out)

        if (savedState != null) {
            position = savedState.getInt(POSITION_KEY)
            adapter.onRestoreInstanceState(savedState)

            if (adapter.mode == FlexibleAdapter.MODE_SINGLE) {
                adapter.clearSelection()
            }
        }

        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recycler: RecyclerView, newState: Int) {
                // Disable swipe refresh when view is not at the top
                val firstPos = (recycler.layoutManager as LinearLayoutManager)
                        .findFirstCompletelyVisibleItemPosition()
                swipe_refresh.isEnabled = firstPos == 0
            }
        })

        // Double the distance required to trigger sync
        swipe_refresh.setDistanceToTriggerSync((2 * 64 * resources.displayMetrics.density).toInt())
        swipe_refresh.setOnRefreshListener {
            if (!LibraryUpdateService.isRunning(activity)) {
                libraryPresenter.categories.getOrNull(position)?.let {
                    LibraryUpdateService.start(activity, true, it)
                    context.toast(R.string.updating_category)
                }
            }
            // It can be a very long operation, so we disable swipe refresh and show a toast.
            swipe_refresh.isRefreshing = false
        }
    }

    override fun onDestroyView() {
        numColumnsSubscription?.unsubscribe()
        searchSubscription?.unsubscribe()
        toggleViewSubscription?.unsubscribe()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()


        libraryMangaSubscription = libraryPresenter.libraryMangaSubject
                .subscribe { onNextLibraryManga(it) }

    }

    override fun onPause() {
        libraryMangaSubscription?.unsubscribe()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(POSITION_KEY, position)
        adapter.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    /**
     * Subscribe to [LibraryMangaEvent]. When an event is received, it updates the content of the
     * adapter.
     *
     * @param event the event received.
     */
    fun onNextLibraryManga(event: LibraryMangaEvent) {
        // Get the categories from the parent fragment.
        val categories = libraryFragment.adapter.categories ?: return

        // When a category is deleted, the index can be greater than the number of categories.
        if (position >= categories.size) return

        // Get the manga list for this category.
        val mangaForCategory = event.getMangaForCategory(categories[position]) ?: emptyList()

        // Update the category with its manga.
        adapter.setItems(mangaForCategory)
    }

    /**
     * Called when a manga is clicked.
     *
     * @param position the position of the element clicked.
     * @return true if the item should be selected, false otherwise.
     */
    override fun onListItemClick(position: Int): Boolean {
        // If the action mode is created and the position is valid, toggle the selection.
        val item = adapter.getItem(position) ?: return false
        if (libraryFragment.actionMode != null) {
            toggleSelection(position)
            return true
        } else {
            openManga(item)
            return false
        }
    }

    /**
     * Called when a manga is long clicked.
     *
     * @param position the position of the element clicked.
     */
    override fun onListItemLongClick(position: Int) {
        libraryFragment.createActionModeIfNeeded()
        toggleSelection(position)
    }

    /**
     * Opens a manga.
     *
     * @param manga the manga to open.
     */
    protected fun openManga(manga: Manga) {
        // Notify the presenter a manga is being opened.
        libraryPresenter.onOpenManga()

        // Create a new activity with the manga.
        val intent = MangaActivity.newIntent(activity, manga)
        startActivity(intent)
    }


    /**
     * Toggles the selection for a manga.
     *
     * @param position the position to toggle.
     */
    private fun toggleSelection(position: Int) {
        val library = libraryFragment

        // Toggle the selection.
        adapter.toggleSelection(position, false)

        // Notify the selection to the presenter.
        library.presenter.setSelection(adapter.getItem(position), adapter.isSelected(position))

        // Get the selected count.
        val count = library.presenter.selectedMangas.size
        if (count == 0) {
            // Destroy action mode if there are no items selected.
            library.destroyActionModeIfNeeded()
        } else {
            // Update action mode with the new selection.
            library.setContextTitle(count)
            library.setVisibilityOfCoverEdit(count)
            library.invalidateActionMode()
        }
    }

    /**
     * Returns a preference for the number of manga per row based on the current orientation.
     *
     * @return the preference.
     */
    fun getColumnsPreferenceForCurrentOrientation(): Preference<Int> {
        return if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
            libraryPresenter.preferences.portraitColumns()
        else
            libraryPresenter.preferences.landscapeColumns()
    }

    /**
     * Sets the mode for the adapter.
     *
     * @param mode the mode to set. It should be MODE_SINGLE or MODE_MULTI.
     */
    fun setSelectionMode(mode: Int) {
        adapter.mode = mode
        if (mode == FlexibleAdapter.MODE_SINGLE) {
            adapter.clearSelection()
        }
    }

    fun onViewModeChange(isList: Boolean) {
        //do nothing if the display does not need to change
        if(isList == displayAsList) return

        //else change view and display mode
        library_switcher.showNext()
        displayAsList = isList
    }

    /**
     * Property to get the library fragment.
     */
    private val libraryFragment: LibraryFragment
        get() = parentFragment as LibraryFragment

    /**
     * Property to get the library presenter.
     */
    private val libraryPresenter: LibraryPresenter
        get() = libraryFragment.presenter

}
