package eu.kanade.tachiyomi.ui.library

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.View
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferenceValues.DisplayMode
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.widget.ExtendedNavigationView
import eu.kanade.tachiyomi.widget.TabbedBottomSheetDialog
import uy.kohesive.injekt.injectLazy

class LibrarySettingsSheet(
    activity: Activity,
    onGroupClickListener: (ExtendedNavigationView.Group) -> Unit
) : TabbedBottomSheetDialog(activity) {

    val filters: Filter
    private val sort: Sort
    private val display: Display

    init {
        filters = Filter(activity)
        filters.onGroupClicked = onGroupClickListener

        sort = Sort(activity)
        sort.onGroupClicked = onGroupClickListener

        display = Display(activity)
        display.onGroupClicked = onGroupClickListener
    }

    override fun getTabViews(): List<View> = listOf(
        filters,
        sort,
        display
    )

    override fun getTabTitles(): List<Int> = listOf(
        R.string.action_filter,
        R.string.action_sort,
        R.string.action_display
    )

    /**
     * Filters group (unread, downloaded, ...).
     */
    inner class Filter @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
        Settings(context, attrs) {

        private val filterGroup = FilterGroup()

        init {
            setGroups(listOf(filterGroup))
        }

        /**
         * Returns true if there's at least one filter from [FilterGroup] active.
         */
        fun hasActiveFilters(): Boolean {
            return filterGroup.items.any { it.checked }
        }

        inner class FilterGroup : Group {

            private val downloaded = Item.CheckboxGroup(R.string.action_filter_downloaded, this)
            private val unread = Item.CheckboxGroup(R.string.action_filter_unread, this)
            private val completed = Item.CheckboxGroup(R.string.completed, this)

            override val header = null
            override val items = listOf(downloaded, unread, completed)
            override val footer = null

            override fun initModels() {
                downloaded.checked = preferences.downloadedOnly().get() || preferences.filterDownloaded().get()
                downloaded.enabled = !preferences.downloadedOnly().get()
                unread.checked = preferences.filterUnread().get()
                completed.checked = preferences.filterCompleted().get()
            }

            override fun onItemClicked(item: Item) {
                item as Item.CheckboxGroup
                item.checked = !item.checked
                when (item) {
                    downloaded -> preferences.filterDownloaded().set(item.checked)
                    unread -> preferences.filterUnread().set(item.checked)
                    completed -> preferences.filterCompleted().set(item.checked)
                }

                adapter.notifyItemChanged(item)
            }
        }
    }

    /**
     * Sorting group (alphabetically, by last read, ...) and ascending or descending.
     */
    inner class Sort @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
        Settings(context, attrs) {

        init {
            setGroups(listOf(SortGroup()))
        }

        inner class SortGroup : Group {

            private val alphabetically = Item.MultiSort(R.string.action_sort_alpha, this)
            private val total = Item.MultiSort(R.string.action_sort_total, this)
            private val lastRead = Item.MultiSort(R.string.action_sort_last_read, this)
            private val lastChecked = Item.MultiSort(R.string.action_sort_last_checked, this)
            private val unread = Item.MultiSort(R.string.action_filter_unread, this)
            private val latestChapter = Item.MultiSort(R.string.action_sort_latest_chapter, this)
            private val dateAdded = Item.MultiSort(R.string.action_sort_date_added, this)

            override val header = null
            override val items =
                listOf(alphabetically, lastRead, lastChecked, unread, total, latestChapter, dateAdded)
            override val footer = null

            override fun initModels() {
                val sorting = preferences.librarySortingMode().get()
                val order = if (preferences.librarySortingAscending().get()) {
                    Item.MultiSort.SORT_ASC
                } else {
                    Item.MultiSort.SORT_DESC
                }

                alphabetically.state =
                    if (sorting == LibrarySort.ALPHA) order else Item.MultiSort.SORT_NONE
                lastRead.state =
                    if (sorting == LibrarySort.LAST_READ) order else Item.MultiSort.SORT_NONE
                lastChecked.state =
                    if (sorting == LibrarySort.LAST_CHECKED) order else Item.MultiSort.SORT_NONE
                unread.state =
                    if (sorting == LibrarySort.UNREAD) order else Item.MultiSort.SORT_NONE
                total.state =
                    if (sorting == LibrarySort.TOTAL) order else Item.MultiSort.SORT_NONE
                latestChapter.state =
                    if (sorting == LibrarySort.LATEST_CHAPTER) order else Item.MultiSort.SORT_NONE
                dateAdded.state =
                    if (sorting == LibrarySort.DATE_ADDED) order else Item.MultiSort.SORT_NONE
            }

            override fun onItemClicked(item: Item) {
                item as Item.MultiStateGroup
                val prevState = item.state

                item.group.items.forEach {
                    (it as Item.MultiStateGroup).state =
                        Item.MultiSort.SORT_NONE
                }
                item.state = when (prevState) {
                    Item.MultiSort.SORT_NONE -> Item.MultiSort.SORT_ASC
                    Item.MultiSort.SORT_ASC -> Item.MultiSort.SORT_DESC
                    Item.MultiSort.SORT_DESC -> Item.MultiSort.SORT_ASC
                    else -> throw Exception("Unknown state")
                }

                preferences.librarySortingMode().set(
                    when (item) {
                        alphabetically -> LibrarySort.ALPHA
                        lastRead -> LibrarySort.LAST_READ
                        lastChecked -> LibrarySort.LAST_CHECKED
                        unread -> LibrarySort.UNREAD
                        total -> LibrarySort.TOTAL
                        latestChapter -> LibrarySort.LATEST_CHAPTER
                        dateAdded -> LibrarySort.DATE_ADDED
                        else -> throw Exception("Unknown sorting")
                    }
                )
                preferences.librarySortingAscending().set(item.state == Item.MultiSort.SORT_ASC)

                item.group.items.forEach { adapter.notifyItemChanged(it) }
            }
        }
    }

    /**
     * Display group, to show the library as a list or a grid.
     */
    inner class Display @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
        Settings(context, attrs) {

        init {
            setGroups(listOf(DisplayGroup(), BadgeGroup(), TabsGroup()))
        }

        inner class DisplayGroup : Group {

            private val compactGrid = Item.Radio(R.string.action_display_grid, this)
            private val comfortableGrid = Item.Radio(R.string.action_display_comfortable_grid, this)
            private val list = Item.Radio(R.string.action_display_list, this)

            override val header = Item.Header(R.string.action_display_mode)
            override val items = listOf(compactGrid, comfortableGrid, list)
            override val footer = null

            override fun initModels() {
                val mode = preferences.libraryDisplayMode().get()
                compactGrid.checked = mode == DisplayMode.COMPACT_GRID
                comfortableGrid.checked = mode == DisplayMode.COMFORTABLE_GRID
                list.checked = mode == DisplayMode.LIST
            }

            override fun onItemClicked(item: Item) {
                item as Item.Radio
                if (item.checked) return

                item.group.items.forEach { (it as Item.Radio).checked = false }
                item.checked = true

                preferences.libraryDisplayMode().set(
                    when (item) {
                        compactGrid -> DisplayMode.COMPACT_GRID
                        comfortableGrid -> DisplayMode.COMFORTABLE_GRID
                        list -> DisplayMode.LIST
                        else -> throw NotImplementedError("Unknown display mode")
                    }
                )

                item.group.items.forEach { adapter.notifyItemChanged(it) }
            }
        }

        inner class BadgeGroup : Group {
            private val downloadBadge = Item.CheckboxGroup(R.string.action_display_download_badge, this)
            private val unreadBadge = Item.CheckboxGroup(R.string.action_display_unread_badge, this)

            override val header = Item.Header(R.string.badges_header)
            override val items = listOf(downloadBadge, unreadBadge)
            override val footer = null

            override fun initModels() {
                downloadBadge.checked = preferences.downloadBadge().get()
                unreadBadge.checked = preferences.unreadBadge().get()
            }

            override fun onItemClicked(item: Item) {
                item as Item.CheckboxGroup
                item.checked = !item.checked
                when (item) {
                    downloadBadge -> preferences.downloadBadge().set((item.checked))
                    unreadBadge -> preferences.unreadBadge().set((item.checked))
                }
                adapter.notifyItemChanged(item)
            }
        }

        inner class TabsGroup : Group {
            private val showTabs = Item.CheckboxGroup(R.string.action_display_show_tabs, this)

            override val header = Item.Header(R.string.tabs_header)
            override val items = listOf(showTabs)
            override val footer = null

            override fun initModels() {
                showTabs.checked = preferences.categoryTabs().get()
            }

            override fun onItemClicked(item: Item) {
                item as Item.CheckboxGroup
                item.checked = !item.checked
                when (item) {
                    showTabs -> preferences.categoryTabs().set((item.checked))
                }
                adapter.notifyItemChanged(item)
            }
        }
    }

    open inner class Settings(context: Context, attrs: AttributeSet?) :
        ExtendedNavigationView(context, attrs) {

        val preferences: PreferencesHelper by injectLazy()
        lateinit var adapter: Adapter

        /**
         * Click listener to notify the parent fragment when an item from a group is clicked.
         */
        var onGroupClicked: (Group) -> Unit = {}

        fun setGroups(groups: List<Group>) {
            adapter = Adapter(groups.map { it.createItems() }.flatten())
            recycler.adapter = adapter

            groups.forEach { it.initModels() }
            addView(recycler)
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
    }
}
