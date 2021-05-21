package eu.kanade.tachiyomi.ui.manga.chapter

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import androidx.core.view.isVisible
import com.bluelinelabs.conductor.Router
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.ui.manga.MangaPresenter
import eu.kanade.tachiyomi.util.view.popupMenu
import eu.kanade.tachiyomi.widget.ExtendedNavigationView
import eu.kanade.tachiyomi.widget.ExtendedNavigationView.Item.TriStateGroup.State
import eu.kanade.tachiyomi.widget.sheet.TabbedBottomSheetDialog

class ChaptersSettingsSheet(
    private val router: Router,
    private val presenter: MangaPresenter,
    private val onGroupClickListener: (ExtendedNavigationView.Group) -> Unit
) : TabbedBottomSheetDialog(router.activity!!) {

    val filters = Filter(router.activity!!)
    private val sort = Sort(router.activity!!)
    private val display = Display(router.activity!!)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        filters.onGroupClicked = onGroupClickListener
        sort.onGroupClicked = onGroupClickListener
        display.onGroupClicked = onGroupClickListener

        binding.menu.isVisible = true
        binding.menu.setOnClickListener { it.post { showPopupMenu(it) } }
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

    private fun showPopupMenu(view: View) {
        view.popupMenu(
            menuRes = R.menu.default_chapter_filter,
            onMenuItemClick = {
                when (itemId) {
                    R.id.set_as_default -> {
                        SetChapterSettingsDialog(presenter.manga).showDialog(router)
                    }
                }
            }
        )
    }

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
            return filterGroup.items.any { it.state != State.IGNORE.value }
        }

        inner class FilterGroup : Group {

            private val downloaded = Item.TriStateGroup(R.string.action_filter_downloaded, this)
            private val unread = Item.TriStateGroup(R.string.action_filter_unread, this)
            private val bookmarked = Item.TriStateGroup(R.string.action_filter_bookmarked, this)

            override val header = null
            override val items = listOf(downloaded, unread, bookmarked)
            override val footer = null

            override fun initModels() {
                if (presenter.forceDownloaded()) {
                    downloaded.state = State.INCLUDE.value
                    downloaded.enabled = false
                } else {
                    downloaded.state = presenter.onlyDownloaded().value
                }
                unread.state = presenter.onlyUnread().value
                bookmarked.state = presenter.onlyBookmarked().value
            }

            override fun onItemClicked(item: Item) {
                item as Item.TriStateGroup
                val newState = when (item.state) {
                    State.IGNORE.value -> State.INCLUDE
                    State.INCLUDE.value -> State.EXCLUDE
                    State.EXCLUDE.value -> State.IGNORE
                    else -> throw Exception("Unknown State")
                }
                item.state = newState.value
                when (item) {
                    downloaded -> presenter.setDownloadedFilter(newState)
                    unread -> presenter.setUnreadFilter(newState)
                    bookmarked -> presenter.setBookmarkedFilter(newState)
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
                    if (sorting == Manga.CHAPTER_SORTING_SOURCE) order else Item.MultiSort.SORT_NONE
                chapterNum.state =
                    if (sorting == Manga.CHAPTER_SORTING_NUMBER) order else Item.MultiSort.SORT_NONE
                uploadDate.state =
                    if (sorting == Manga.CHAPTER_SORTING_UPLOAD_DATE) order else Item.MultiSort.SORT_NONE
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
                    source -> presenter.setSorting(Manga.CHAPTER_SORTING_SOURCE)
                    chapterNum -> presenter.setSorting(Manga.CHAPTER_SORTING_NUMBER)
                    uploadDate -> presenter.setSorting(Manga.CHAPTER_SORTING_UPLOAD_DATE)
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
                displayTitle.checked = mode == Manga.CHAPTER_DISPLAY_NAME
                displayChapterNum.checked = mode == Manga.CHAPTER_DISPLAY_NUMBER
            }

            override fun onItemClicked(item: Item) {
                item as Item.Radio
                if (item.checked) return

                item.group.items.forEach { (it as Item.Radio).checked = false }
                item.checked = true

                when (item) {
                    displayTitle -> presenter.setDisplayMode(Manga.CHAPTER_DISPLAY_NAME)
                    displayChapterNum -> presenter.setDisplayMode(Manga.CHAPTER_DISPLAY_NUMBER)
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
