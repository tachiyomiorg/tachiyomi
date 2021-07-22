package eu.kanade.tachiyomi.ui.library

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import androidx.core.view.isVisible
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.Router
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.util.view.setTooltip
import eu.kanade.tachiyomi.widget.ExtendedNavigationView
import eu.kanade.tachiyomi.widget.ExtendedNavigationView.Item.TriDisplayGroup.State
import eu.kanade.tachiyomi.widget.sheet.TabbedBottomSheetDialog

class ChangeMangaCategoriesSheet<T>(
    // TODO: I am not familiar with Android so I just throw everything in constructor
    target: T,
    private val router: Router,
    private val mangas: List<Manga>,
    private val categories: List<Category>,
    private val commonIndexes: Array<Int>,
    private val mixIndexes: Array<Int>,
    private val onGroupClickListener: (ExtendedNavigationView.Group) -> Unit
) : TabbedBottomSheetDialog(router.activity!!) where T : Controller, T : ChangeMangaCategoriesSheet.Listener {

    private val selections = Selection(router.activity!!)
    private val targetController = target

    interface Listener {
        fun updateCategoriesForMangas(mangas: List<Manga>, newCommon: List<Category>, oldCommon: List<Category> = emptyList<Category>())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selections.onGroupClicked = onGroupClickListener
        // Reuse Menu Option as Apply button
        binding.menu.setImageResource(R.drawable.ic_check_24dp)
        binding.menu.isVisible = true
        binding.menu.setTooltip(R.string.action_update)
        binding.menu.setOnClickListener {
            val newCommon = selections.getCommonSelections().map { categories[it] }.toList()
            val newMix = selections.getMixSelections().map { categories[it] }.toList()
            (targetController as? Listener)?.updateCategoriesForMangas(mangas, newCommon, newMix)
            // TODO: I am not sure how to close after finished
            this.dismiss()
        }
    }

    override fun getTabViews(): List<View> = listOf(
        selections
    )

    override fun getTabTitles(): List<Int> = listOf(
        R.string.action_move_category
    )

    /**
     * Selections group (unread, downloaded, ...).
     */

    inner class Selection @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
        Settings(context, attrs) {

        private val selectionGroup = SelectionGroup()

        init {
            setGroups(listOf(selectionGroup))
        }

        /**
         * Returns true if there's at least one selection from [SelectionGroup] active.
         */
        fun hasActiveSelections(): Boolean {
            return selectionGroup.items.any { it.state != State.IGNORE.value }
        }

        /**
         * Returns list of Common selection from [SelectionGroup] active.
         */
        fun getCommonSelections(): Array<Int> {
            val selected = selectionGroup.items
                .mapIndexed { index, triDisplayGroup -> index to triDisplayGroup.state }
                .toMap()
            return selected.filter { it.value == State.COMMON.value }.keys.toTypedArray()
        }

        /**
         * Returns list of Mix selection from [SelectionGroup] active.
         */
        fun getMixSelections(): Array<Int> {
            val selected = selectionGroup.items
                .mapIndexed { index, triDisplayGroup -> index to triDisplayGroup.state }
                .toMap()
            return selected.filter { it.value == State.MIX.value }.keys.toTypedArray()
        }

        inner class SelectionGroup : Group {

            override val header = null
            override val items = categories.map { Item.TriDisplayGroup(0, group = this, resTitleString = it.name) }
            override val footer = null

            override fun initModels() {
                // Set init state per Common or Mix list
                items.forEachIndexed { index, triDisplayGroup ->
                    triDisplayGroup.state =
                        when (commonIndexes.contains(index)) {
                            true -> State.COMMON.value
                            false -> when (mixIndexes.contains(index)) {
                                true -> State.MIX.value
                                false -> State.IGNORE.value
                            }
                        }
                }
            }

            override fun onItemClicked(item: Item) {
                item as Item.TriDisplayGroup
                // Only item part of Mix list, it would have extra sequence Ignore, Mix, Common
                val newState = when (item.state) {
                    State.IGNORE.value -> when (mixIndexes.contains(items.indexOf(item))) {
                        true -> State.MIX
                        false -> State.COMMON
                    }
                    State.COMMON.value -> State.IGNORE
                    State.MIX.value -> State.COMMON
                    else -> throw Exception("Unknown State")
                }
                item.state = newState.value
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
