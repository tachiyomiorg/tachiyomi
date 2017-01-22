package eu.kanade.tachiyomi.ui.library

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.widget.FrameLayout
import eu.davidea.flexibleadapter4.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.ui.base.adapter.FlexibleViewHolder
import eu.kanade.tachiyomi.ui.manga.MangaActivity
import eu.kanade.tachiyomi.util.inflate
import eu.kanade.tachiyomi.util.toast
import eu.kanade.tachiyomi.widget.AutofitRecyclerView
import kotlinx.android.synthetic.main.item_library_category.view.*
import rx.Subscription
import uy.kohesive.injekt.injectLazy

/**
 * Fragment containing the library manga for a certain category.
 * Uses R.layout.fragment_library_category.
 */
class LibraryCategoryView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null)
: FrameLayout(context, attrs), FlexibleViewHolder.OnListItemClickListener {

    /**
     * Preferences.
     */
    private val preferences: PreferencesHelper by injectLazy()

    /**
     * The fragment containing this view.
     */
    private lateinit var fragment: LibraryFragment

    /**
     * Category for this view.
     */
    lateinit var category: Category
        private set

    /**
     * Recycler view of the list of manga.
     */
    private lateinit var recycler: RecyclerView

    /**
     * Adapter to hold the manga in this category.
     */
    private lateinit var adapter: LibraryCategoryAdapter

    /**
     * Subscription for the library manga.
     */
    private var libraryMangaSubscription: Subscription? = null

    /**
     * Subscription of the library search.
     */
    private var searchSubscription: Subscription? = null

    /**
     * Subscription of the library selections.
     */
    private var selectionSubscription: Subscription? = null

    fun onCreate(fragment: LibraryFragment) {
        this.fragment = fragment

        recycler = if (preferences.libraryAsList().getOrDefault()) {
            (swipe_refresh.inflate(R.layout.library_list_recycler) as RecyclerView).apply {
                layoutManager = LinearLayoutManager(context)
            }
        } else {
            (swipe_refresh.inflate(R.layout.library_grid_recycler) as AutofitRecyclerView).apply {
                spanCount = fragment.mangaPerRow
            }
        }

        adapter = LibraryCategoryAdapter(this)

        recycler.setHasFixedSize(true)
        recycler.adapter = adapter
        swipe_refresh.addView(recycler)

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
            if (!LibraryUpdateService.isRunning(context)) {
                LibraryUpdateService.start(context, category)
                context.toast(R.string.updating_category)
            }
            // It can be a very long operation, so we disable swipe refresh and show a toast.
            swipe_refresh.isRefreshing = false
        }
    }

    fun onBind(category: Category) {
        this.category = category

        val presenter = fragment.presenter

        searchSubscription = presenter.searchSubject.subscribe { text ->
            adapter.searchText = text
            adapter.updateDataSet()
        }

        adapter.mode = if (presenter.selectedMangas.isNotEmpty()) {
            FlexibleAdapter.MODE_MULTI
        } else {
            FlexibleAdapter.MODE_SINGLE
        }

        libraryMangaSubscription = presenter.libraryMangaSubject
                .subscribe { onNextLibraryManga(it) }

        selectionSubscription = presenter.selectionSubject
                .subscribe { onSelectionChanged(it) }
    }

    fun onRecycle() {
        adapter.setItems(emptyList())
        adapter.clearSelection()
    }

    override fun onDetachedFromWindow() {
        searchSubscription?.unsubscribe()
        libraryMangaSubscription?.unsubscribe()
        selectionSubscription?.unsubscribe()
        super.onDetachedFromWindow()
    }

    /**
     * Subscribe to [LibraryMangaEvent]. When an event is received, it updates the content of the
     * adapter.
     *
     * @param event the event received.
     */
    fun onNextLibraryManga(event: LibraryMangaEvent) {
        // Get the manga list for this category.
        val mangaForCategory = event.getMangaForCategory(category).orEmpty()

        // Update the category with its manga.
        adapter.setItems(mangaForCategory)

        if (adapter.mode == FlexibleAdapter.MODE_MULTI) {
            fragment.presenter.selectedMangas.forEach { manga ->
                val position = adapter.indexOf(manga)
                if (position != -1 && !adapter.isSelected(position)) {
                    adapter.toggleSelection(position)
                    (recycler.findViewHolderForItemId(manga.id!!) as? LibraryHolder)?.toggleActivation()
                }
            }
        }
    }

    /**
     * Subscribe to [LibrarySelectionEvent]. When an event is received, it updates the selection
     * depending on the type of event received.
     *
     * @param event the selection event received.
     */
    private fun onSelectionChanged(event: LibrarySelectionEvent) {
        when (event) {
            is LibrarySelectionEvent.Selected -> {
                if (adapter.mode != FlexibleAdapter.MODE_MULTI) {
                    adapter.mode = FlexibleAdapter.MODE_MULTI
                }
                findAndToggleSelection(event.manga)
            }
            is LibrarySelectionEvent.Unselected -> {
                findAndToggleSelection(event.manga)
                if (fragment.presenter.selectedMangas.isEmpty()) {
                    adapter.mode = FlexibleAdapter.MODE_SINGLE
                }
            }
            is LibrarySelectionEvent.Cleared -> {
                adapter.mode = FlexibleAdapter.MODE_SINGLE
                adapter.clearSelection()
            }
        }
    }

    /**
     * Toggles the selection for the given manga and updates the view if needed.
     *
     * @param manga the manga to toggle.
     */
    private fun findAndToggleSelection(manga: Manga) {
        val position = adapter.indexOf(manga)
        if (position != -1) {
            adapter.toggleSelection(position)
            (recycler.findViewHolderForItemId(manga.id!!) as? LibraryHolder)?.toggleActivation()
        }
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
        if (adapter.mode == FlexibleAdapter.MODE_MULTI) {
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
        fragment.createActionModeIfNeeded()
        toggleSelection(position)
    }

    /**
     * Opens a manga.
     *
     * @param manga the manga to open.
     */
    private fun openManga(manga: Manga) {
        // Notify the presenter a manga is being opened.
        fragment.presenter.onOpenManga()

        // Create a new activity with the manga.
        val intent = MangaActivity.newIntent(context, manga)
        fragment.startActivity(intent)
    }


    /**
     * Tells the presenter to toggle the selection for the given position.
     *
     * @param position the position to toggle.
     */
    private fun toggleSelection(position: Int) {
        val manga = adapter.getItem(position) ?: return

        fragment.presenter.setSelection(manga, !adapter.isSelected(position))
        fragment.invalidateActionMode()
    }

}
