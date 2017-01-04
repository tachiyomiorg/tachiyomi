package eu.kanade.tachiyomi.ui.library

import android.content.Context
import android.util.AttributeSet
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.widget.ExtendedNavigationView
import eu.kanade.tachiyomi.widget.ExtendedNavigationView.Item.MultiSort.Companion.SORT_ASC
import eu.kanade.tachiyomi.widget.ExtendedNavigationView.Item.MultiSort.Companion.SORT_DESC
import eu.kanade.tachiyomi.widget.ExtendedNavigationView.Item.MultiSort.Companion.SORT_NONE
import uy.kohesive.injekt.injectLazy

/**
 * The navigation view shown in a drawer with the different options to show the library.
 */
class LibraryNavigationView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null)
: ExtendedNavigationView(context, attrs) {

    /**
     * Preferences helper.
     */
    private val preferences: PreferencesHelper by injectLazy()

    /**
     * List of groups shown in the view.
     */
    private val groups = listOf(FilterGroup(), SortGroup(),  DisplayGroup())

    /**
     * Adapter instance.
     */
    private val adapter = Adapter(groups.map { it.createItems() }.flatten())

    /**
     * Click listener to notify the parent fragment when an item from a group is clicked.
     */
    var onGroupClicked: (Group) -> Unit = {}

    init {
        recycler.adapter = adapter
        addView(recycler)

        groups.forEach { it.initModels() }
    }

    /**
     * Returns true if there's at least one filter from [FilterGroup] active.
     */
    fun hasActiveFilters(): Boolean {
        return (groups[0] as FilterGroup).items.any { it.checked }
    }

    /**
     * Adapter of the recycler view.
     */
    inner class Adapter(items: List<Item>) : ExtendedNavigationView.Adapter(items) {

        override fun onItemClicked(item: Item) {
            if (item is GroupedItem) {
                item.group.onItemClicked(item)
                onGroupClicked(item.group)
            }
        }
        
    }

    /**
     * Filters group (unread, downloaded, ...).
     */
    inner class FilterGroup : Group {

        private val downloaded = Item.CheckboxGroup(R.string.action_filter_downloaded, this)

        private val unread = Item.CheckboxGroup(R.string.action_filter_unread, this)

        override val items = listOf(downloaded, unread)

        override val header = Item.Header(R.string.action_filter)

        override val footer = Item.Separator()

        override fun initModels() {
            downloaded.checked = preferences.filterDownloaded().getOrDefault()
            unread.checked = preferences.filterUnread().getOrDefault()
        }

        override fun onItemClicked(item: Item) {
            item as Item.CheckboxGroup
            item.checked = !item.checked
            when (item) {
                downloaded -> preferences.filterDownloaded().set(item.checked)
                unread -> preferences.filterUnread().set(item.checked)
            }

            adapter.notifyItemChanged(item)
        }

    }

    /**
     * Sorting group (alphabetically, by last read, ...) and ascending or descending.
     */
    inner class SortGroup : Group {

        private val alphabetically = Item.MultiSort(R.string.action_sort_alpha, this)

        private val lastRead = Item.MultiSort(R.string.action_sort_last_read, this)

        private val lastUpdated = Item.MultiSort(R.string.action_sort_last_updated, this)

        private val unread = Item.MultiSort(R.string.action_filter_unread, this)

        override val items = listOf(alphabetically, lastRead, lastUpdated, unread)

        override val header = Item.Header(R.string.action_sort)

        override val footer = Item.Separator()

        override fun initModels() {
            val sorting = preferences.librarySortingMode().getOrDefault()
            val order = if (preferences.librarySortingAscending().getOrDefault())
                SORT_ASC else SORT_DESC

            alphabetically.state = if (sorting == LibrarySort.ALPHA) order else SORT_NONE
            lastRead.state = if (sorting == LibrarySort.LAST_READ) order else SORT_NONE
            lastUpdated.state = if (sorting == LibrarySort.LAST_UPDATED) order else SORT_NONE
            unread.state = if (sorting == LibrarySort.UNREAD) order else SORT_NONE
        }

        override fun onItemClicked(item: Item) {
            item as Item.MultiStateGroup
            val prevState = item.state

            item.group.items.forEach { (it as Item.MultiStateGroup).state = SORT_NONE }
            item.state = when (prevState) {
                SORT_NONE -> SORT_ASC
                SORT_ASC -> SORT_DESC
                SORT_DESC -> SORT_ASC
                else -> throw Exception("Unknown state")
            }

            preferences.librarySortingMode().set(when (item) {
                alphabetically -> LibrarySort.ALPHA
                lastRead -> LibrarySort.LAST_READ
                lastUpdated -> LibrarySort.LAST_UPDATED
                unread -> LibrarySort.UNREAD
                else -> throw Exception("Unknown sorting")
            })
            preferences.librarySortingAscending().set(if (item.state == SORT_ASC) true else false)

            item.group.items.forEach { adapter.notifyItemChanged(it) }
        }

    }

    /**
     * Display group, to show the library as a list or a grid.
     */
    inner class DisplayGroup : Group {

        private val grid = Item.Radio(R.string.action_display_grid, this)

        private val list = Item.Radio(R.string.action_display_list, this)

        override val items = listOf(grid, list)

        override val header = Item.Header(R.string.action_display)

        override val footer = null

        override fun initModels() {
            val asList = preferences.libraryAsList().getOrDefault()
            grid.checked = !asList
            list.checked = asList
        }

        override fun onItemClicked(item: Item) {
            item as Item.Radio
            if (item.checked) return

            item.group.items.forEach { (it as Item.Radio).checked = false }
            item.checked = true

            preferences.libraryAsList().set(if (item == list) true else false)

            item.group.items.forEach { adapter.notifyItemChanged(it) }
        }

    }

}