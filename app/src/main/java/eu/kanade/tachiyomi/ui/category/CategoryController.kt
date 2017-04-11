package eu.kanade.tachiyomi.ui.category

import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import com.jakewharton.rxbinding.view.clicks
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.helpers.UndoHelper
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import kotlinx.android.synthetic.main.categories_controller.view.*

class CategoryController : NucleusController<CategoryPresenter>(),
        ActionMode.Callback,
        FlexibleAdapter.OnItemClickListener,
        FlexibleAdapter.OnItemLongClickListener,
        UndoHelper.OnUndoListener {

    /**
     * Object used to show actionMode toolbar.
     */
    private var actionMode: ActionMode? = null

    /**
     * Adapter containing category items.
     */
    private var adapter: CategoryAdapter? = null

    override fun createPresenter() = CategoryPresenter()

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.categories_controller, container, false)
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        with(view) {
            adapter = CategoryAdapter(this@CategoryController)
            recycler.layoutManager = LinearLayoutManager(context)
            recycler.setHasFixedSize(true)
            recycler.adapter = adapter
            adapter?.isHandleDragEnabled = true

            fab.clicks().subscribeUntilDestroy {
                CategoryCreateDialog(this@CategoryController).showDialog(router, null)
            }
        }
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        actionMode = null
        adapter = null
    }

    override fun getTitle(): String? {
        return resources?.getString(R.string.action_edit_categories)
    }

    /**
     * Fill adapter with category items
     *
     * @param categories list containing categories
     */
    fun setCategories(categories: List<CategoryItem>) {
        actionMode?.finish()
        adapter?.updateDataSet(categories.toMutableList())
        val selected = categories.filter { it.isSelected }
        if (selected.isNotEmpty()) {
            selected.forEach { onItemLongClick(categories.indexOf(it)) }
        }
    }

    /**
     * Show dialog which let user change category name.
     *
     * @param category category that will be edited.
     */
    private fun editCategory(category: Category) {
        CategoryRenameDialog(category, this).showDialog(router, null)
    }

    /**
     * Called when action mode item clicked.
     *
     * @param actionMode action mode toolbar.
     * @param menuItem selected menu item.
     *
     * @return action mode item clicked exist status
     */
    override fun onActionItemClicked(actionMode: ActionMode, menuItem: MenuItem): Boolean {
        val adapter = adapter ?: return false

        when (menuItem.itemId) {
            R.id.action_delete -> {
                UndoHelper(adapter, this)
                        .withAction(UndoHelper.ACTION_REMOVE, object : UndoHelper.OnActionListener {
                            override fun onPreAction(): Boolean {
                                adapter.selectedPositions.forEach { adapter.getItem(it).isSelected = false }
                                return false
                            }

                            override fun onPostAction() {
                                actionMode.finish()
                            }
                        })
                        .remove(adapter.selectedPositions, view!!,
                                R.string.snack_categories_deleted, R.string.action_undo, 3000)
            }
            R.id.action_edit -> {
                // Edit selected category
                if (adapter.selectedItemCount == 1) {
                    val position = adapter.selectedPositions.first()
                    editCategory(adapter.getItem(position).category)
                }
            }
            else -> return false
        }
        return true
    }

    /**
     * Inflate menu when action mode selected.
     *
     * @param mode ActionMode object
     * @param menu Menu object
     *
     * @return true
     */
    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        // Inflate menu.
        mode.menuInflater.inflate(R.menu.category_selection, menu)
        // Enable adapter multi selection.
        adapter?.mode = FlexibleAdapter.MODE_MULTI
        return true
    }

    /**
     * Called each time the action mode is shown.
     * Always called after onCreateActionMode
     *
     * @return false
     */
    override fun onPrepareActionMode(actionMode: ActionMode, menu: Menu): Boolean {
        val adapter = adapter ?: return false
        val count = adapter.selectedItemCount
        actionMode.title = resources?.getString(R.string.label_selected, count)

        // Show edit button only when one item is selected
        val editItem = actionMode.menu.findItem(R.id.action_edit)
        editItem.isVisible = count == 1
        return true
    }

    /**
     * Called when action mode destroyed.
     *
     * @param mode ActionMode object.
     */
    override fun onDestroyActionMode(mode: ActionMode?) {
        // Reset adapter to single selection
        adapter?.mode = FlexibleAdapter.MODE_IDLE
        adapter?.clearSelection()
        actionMode = null
    }

    /**
     * Called when item in list is clicked.
     *
     * @param position position of clicked item.
     */
    override fun onItemClick(position: Int): Boolean {
        // Check if action mode is initialized and selected item exist.
        if (actionMode != null && position != RecyclerView.NO_POSITION) {
            toggleSelection(position)
            return true
        } else {
            return false
        }
    }

    /**
     * Called when item long clicked
     *
     * @param position position of clicked item.
     */
    override fun onItemLongClick(position: Int) {
        val activity = activity as? AppCompatActivity ?: return

        // Check if action mode is initialized.
        if (actionMode == null) {
            // Initialize action mode
            actionMode = activity.startSupportActionMode(this)
        }

        // Set item as selected
        toggleSelection(position)
    }

    /**
     * Toggle the selection state of an item.
     * If the item was the last one in the selection and is unselected, the ActionMode is finished.
     */
    private fun toggleSelection(position: Int) {
        val adapter = adapter ?: return

        //Mark the position selected
        adapter.toggleSelection(position)

        if (adapter.selectedItemCount == 0) {
            actionMode?.finish()
        } else {
            actionMode?.invalidate()
        }
    }

    /**
     * Called when an item is released from a drag.
     */
    fun onItemReleased() {
        val adapter = adapter ?: return
        val categories = (0..adapter.itemCount-1).map { adapter.getItem(it).category }
        presenter.reorderCategories(categories)
    }

    /**
     * Called when the undo action is clicked in the snackbar.
     */
    override fun onUndoConfirmed(action: Int) {
        adapter?.restoreDeletedItems()
    }

    /**
     * Called when the time to restore the items expires.
     */
    override fun onDeleteConfirmed(action: Int) {
        val adapter = adapter ?: return
        presenter.deleteCategories(adapter.deletedItems.map { it.category })
    }

}