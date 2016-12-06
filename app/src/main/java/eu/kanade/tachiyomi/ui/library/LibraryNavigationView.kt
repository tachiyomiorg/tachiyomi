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

class LibraryNavigationView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null)
: ExtendedNavigationView(context, attrs) {

    private val preferences: PreferencesHelper by injectLazy()

    private val groups = listOf(FilterGroup(), SortGroup(),  DisplayGroup())

    private val items = groups.map { it.createItems() }.flatten()

    private val adapter = Adapter()

    var onGroupClicked: (Group) -> Unit = {}

    init {
        recycler.adapter = adapter

        groups.forEach { it.initModels() }
    }

    inner class Adapter() : ExtendedNavigationView.Adapter(items) {

        override fun onItemClicked(item: Item) {
            if (item is GroupedItem) {
                item.group.onItemClicked(item)
                onGroupClicked(item.group)
            }
        }
        
    }

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

    inner class SortGroup : Group {

        private val alphabetically = Item.MultiSort(R.string.action_sort_alpha, this)

        private val lastRead = Item.MultiSort(R.string.action_sort_last_read, this)

        private val lastUpdated = Item.MultiSort(R.string.action_sort_last_updated, this)

        override val items = listOf(alphabetically, lastRead, lastUpdated)

        override val header = Item.Header(R.string.action_sort)

        override val footer = Item.Separator()

        override fun initModels() {
            val sorting = preferences.librarySortingMode().getOrDefault()
            val order = if (preferences.librarySortingAscending().getOrDefault())
                SORT_ASC else SORT_DESC

            alphabetically.state = if (sorting == LibrarySort.ALPHA) order else SORT_NONE
            lastRead.state = if (sorting == LibrarySort.LAST_READ) order else SORT_NONE
            lastUpdated.state = if (sorting == LibrarySort.LAST_UPDATED) order else SORT_NONE
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
                else -> throw Exception("Unknown sorting")
            })
            preferences.librarySortingAscending().set(if (item.state == SORT_ASC) true else false)

            item.group.items.forEach { adapter.notifyItemChanged(it) }
        }

    }

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