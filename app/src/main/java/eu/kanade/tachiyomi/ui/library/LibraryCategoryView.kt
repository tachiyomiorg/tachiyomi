package eu.kanade.tachiyomi.ui.library

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.SelectableAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.util.inflate
import eu.kanade.tachiyomi.util.plusAssign
import eu.kanade.tachiyomi.util.toast
import eu.kanade.tachiyomi.widget.AutofitRecyclerView
import kotlinx.android.synthetic.main.library_category.view.swipe_refresh
import rx.subscriptions.CompositeSubscription
import uy.kohesive.injekt.injectLazy

/**
 * Fragment containing the library manga for a certain category.
 */
class LibraryCategoryView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
        FrameLayout(context, attrs),
        FlexibleAdapter.OnItemClickListener,
        FlexibleAdapter.OnItemLongClickListener {

    /**
     * Preferences.
     */
    private val preferences: PreferencesHelper by injectLazy()

    /**
     * The fragment containing this view.
     */
    private lateinit var controller: LibraryController

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
     * Subscriptions while the view is bound.
     */
    private var subscriptions = CompositeSubscription()

    private var lastClickPosition = -1

    fun onCreate(controller: LibraryController) {
        this.controller = controller

        recycler = if (preferences.libraryAsList().getOrDefault()) {
            (swipe_refresh.inflate(R.layout.library_list_recycler) as RecyclerView).apply {
                layoutManager = LinearLayoutManager(context)
            }
        } else {
            (swipe_refresh.inflate(R.layout.library_grid_recycler) as AutofitRecyclerView).apply {
                spanCount = controller.mangaPerRow
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
                swipe_refresh.isEnabled = firstPos <= 0
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

        adapter.mode = if (controller.selectedMangas.isNotEmpty()) {
            SelectableAdapter.Mode.MULTI
        } else {
            SelectableAdapter.Mode.SINGLE
        }

        subscriptions += controller.searchRelay
                .doOnNext { adapter.setFilter(it) }
                .skip(1)
                .subscribe { adapter.performFilter() }

        subscriptions += controller.libraryMangaRelay
                .subscribe { onNextLibraryManga(it) }

        subscriptions += controller.selectionRelay
                .subscribe { onSelectionChanged(it) }

        subscriptions += controller.selectAllRelay
                .filter { it == category.id }
                .subscribe {
                    adapter.currentItems.forEach { item ->
                        controller.setSelection(item.manga, true)
                    }
                    controller.invalidateActionMode()
                }
    }

    fun onRecycle() {
        adapter.setItems(emptyList())
        adapter.clearSelection()
        unsubscribe()
    }

    fun unsubscribe() {
        subscriptions.clear()
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

        if (adapter.mode == SelectableAdapter.Mode.MULTI) {
            controller.selectedMangas.forEach { manga ->
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
                if (adapter.mode != SelectableAdapter.Mode.MULTI) {
                    adapter.mode = SelectableAdapter.Mode.MULTI
                }
                findAndToggleSelection(event.manga)
            }
            is LibrarySelectionEvent.Unselected -> {
                findAndToggleSelection(event.manga)
                if (adapter.indexOf(event.manga) != -1) lastClickPosition = -1
                if (controller.selectedMangas.isEmpty()) {
                    adapter.mode = SelectableAdapter.Mode.SINGLE
                }
            }
            is LibrarySelectionEvent.Cleared -> {
                adapter.mode = SelectableAdapter.Mode.SINGLE
                adapter.clearSelection()
                lastClickPosition = -1
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
    override fun onItemClick(view: View?, position: Int): Boolean {
        // If the action mode is created and the position is valid, toggle the selection.
        val item = adapter.getItem(position) ?: return false
        if (adapter.mode == SelectableAdapter.Mode.MULTI) {
            lastClickPosition = position
            toggleSelection(position)
            return true
        } else {
            openManga(item.manga)
            return false
        }
    }

    /**
     * Called when a manga is long clicked.
     *
     * @param position the position of the element clicked.
     */
    override fun onItemLongClick(position: Int) {
        controller.createActionModeIfNeeded()
        when {
            lastClickPosition == -1 -> setSelection(position)
            lastClickPosition > position -> for (i in position until lastClickPosition)
                setSelection(i)
            lastClickPosition < position -> for (i in lastClickPosition + 1..position)
                setSelection(i)
            else -> setSelection(position)
        }
        lastClickPosition = position
    }

    /**
     * Opens a manga.
     *
     * @param manga the manga to open.
     */
    private fun openManga(manga: Manga) {
        controller.openManga(manga)
    }

    /**
     * Tells the presenter to toggle the selection for the given position.
     *
     * @param position the position to toggle.
     */
    private fun toggleSelection(position: Int) {
        val item = adapter.getItem(position) ?: return

        controller.setSelection(item.manga, !adapter.isSelected(position))
        controller.invalidateActionMode()
    }


    /**
     * Tells the presenter to set the selection for the given position.
     *
     * @param position the position to toggle.
     */
    private fun setSelection(position: Int) {
        val item = adapter.getItem(position) ?: return

        controller.setSelection(item.manga, true)
        controller.invalidateActionMode()
    }

}
