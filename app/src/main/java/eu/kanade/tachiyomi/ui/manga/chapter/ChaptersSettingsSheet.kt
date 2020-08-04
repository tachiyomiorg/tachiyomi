package eu.kanade.tachiyomi.ui.manga.chapter

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.View
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.ui.manga.MangaPresenter
import eu.kanade.tachiyomi.widget.ExtendedNavigationView
import eu.kanade.tachiyomi.widget.TabbedBottomSheetDialog

class ChaptersSettingsSheet(
    activity: Activity,
    private val presenter: MangaPresenter,
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

            private val read = Item.CheckboxGroup(R.string.action_filter_read, this)
            private val unread = Item.CheckboxGroup(R.string.action_filter_unread, this)
            private val downloaded = Item.CheckboxGroup(R.string.action_filter_downloaded, this)
            private val bookmarked = Item.CheckboxGroup(R.string.action_filter_bookmarked, this)

            override val header = null
            override val items = listOf(read, unread, downloaded, bookmarked)
            override val footer = null

            override fun initModels() {
                read.checked = presenter.onlyRead()
                unread.checked = presenter.onlyUnread()
                downloaded.checked = presenter.onlyDownloaded()
                downloaded.enabled = !presenter.forceDownloaded()
                bookmarked.checked = presenter.onlyBookmarked()
            }

            override fun onItemClicked(item: Item) {
                item as Item.CheckboxGroup
                item.checked = !item.checked
                when (item) {
                    read -> presenter.setReadFilter(item.checked)
                    unread -> presenter.setUnreadFilter(item.checked)
                    downloaded -> presenter.setDownloadedFilter(item.checked)
                    bookmarked -> presenter.setBookmarkedFilter(item.checked)
                }

                initModels()
                item.group.items.forEach { adapter.notifyItemChanged(it) }
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

            private val source = Item.MultiSort(R.string.sort_by_source, this)
            private val chapterNum = Item.MultiSort(R.string.sort_by_number, this)
            private val uploadDate = Item.MultiSort(R.string.sort_by_upload_date, this)

            override val header = null
            override val items = listOf(source, uploadDate, chapterNum)
            override val footer = null

            override fun initModels() {
                val sorting = presenter.manga.sorting
                val order = if (presenter.manga.sortDescending()) {
                    Item.MultiSort.SORT_DESC
                } else {
                    Item.MultiSort.SORT_ASC
                }

                source.state =
                    if (sorting == Manga.SORTING_SOURCE) order else Item.MultiSort.SORT_NONE
                chapterNum.state =
                    if (sorting == Manga.SORTING_NUMBER) order else Item.MultiSort.SORT_NONE
                uploadDate.state =
                    if (sorting == Manga.SORTING_UPLOAD_DATE) order else Item.MultiSort.SORT_NONE
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

                when (item) {
                    source -> presenter.setSorting(Manga.SORTING_SOURCE)
                    chapterNum -> presenter.setSorting(Manga.SORTING_NUMBER)
                    uploadDate -> presenter.setSorting(Manga.SORTING_UPLOAD_DATE)
                    else -> throw Exception("Unknown sorting")
                }

                presenter.reverseSortOrder()

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
            setGroups(listOf(DisplayGroup()))
        }

        inner class DisplayGroup : Group {

            private val displayTitle = Item.Radio(R.string.show_title, this)
            private val displayChapterNum = Item.Radio(R.string.show_chapter_number, this)

            override val header = null
            override val items = listOf(displayTitle, displayChapterNum)
            override val footer = null

            override fun initModels() {
                val mode = presenter.manga.displayMode
                displayTitle.checked = mode == Manga.DISPLAY_NAME
                displayChapterNum.checked = mode == Manga.DISPLAY_NUMBER
            }

            override fun onItemClicked(item: Item) {
                item as Item.Radio
                if (item.checked) return

                item.group.items.forEach { (it as Item.Radio).checked = false }
                item.checked = true

                when (item) {
                    displayTitle -> presenter.setDisplayMode(Manga.DISPLAY_NAME)
                    displayChapterNum -> presenter.setDisplayMode(Manga.DISPLAY_NUMBER)
                    else -> throw NotImplementedError("Unknown display mode")
                }

                item.group.items.forEach { adapter.notifyItemChanged(it) }
            }
        }
    }

    open inner class Settings(context: Context, attrs: AttributeSet?) :
        ExtendedNavigationView(context, attrs) {

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
