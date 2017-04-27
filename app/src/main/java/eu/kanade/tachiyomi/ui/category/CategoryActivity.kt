package eu.kanade.tachiyomi.ui.category

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.view.ActionMode
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.helpers.UndoHelper
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.ui.base.activity.BaseRxActivity
import kotlinx.android.synthetic.main.activity_edit_categories.*
import kotlinx.android.synthetic.main.toolbar.*
import nucleus.factory.RequiresPresenter


/**
 * Activity that shows categories.
 * Uses R.layout.activity_edit_categories.
 * UI related actions should be called from here.
 */
@RequiresPresenter(CategoryPresenter::class)
class CategoryActivity :
        BaseRxActivity<CategoryPresenter>(),
        ActionMode.Callback,
        FlexibleAdapter.OnItemClickListener,
        FlexibleAdapter.OnItemLongClickListener,
        UndoHelper.OnUndoListener {

    /**
     * Object used to show actionMode toolbar.
     */
    var actionMode: ActionMode? = null

    /**
     * Adapter containing category items.
     */
    private lateinit var adapter: CategoryAdapter

    companion object {
        /**
         * Create new CategoryActivity intent.
         *
         * @param context context information.
         */
        fun newIntent(context: Context): Intent {
            return Intent(context, CategoryActivity::class.java)
        }
    }

    override fun onCreate(savedState: Bundle?) {
        setAppTheme()
        super.onCreate(savedState)

        // Inflate activity_edit_categories.xml.
        setContentView(R.layout.activity_edit_categories)

        // Setup the toolbar.
        setupToolbar(toolbar)

        // Get new adapter.
        adapter = CategoryAdapter(this)

        // Create view and inject category items into view
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.setHasFixedSize(true)
        recycler.adapter = adapter

        adapter.isHandleDragEnabled = true

        // Create OnClickListener for creating new category
        fab.setOnClickListener {
            MaterialDialog.Builder(this)
                    .title(R.string.action_add_category)
                    .negativeText(android.R.string.cancel)
                    .input(R.string.name, 0, false)
                    { dialog, input -> presenter.createCategory(input.toString()) }
                    .show()
        }
    }

    /**
     * Fill adapter with category items
     *
     * @param categories list containing categories
     */
    fun setCategories(categories: List<CategoryItem>) {
        actionMode?.finish()
        adapter.updateDataSet(categories.toMutableList())
        val selected = categories.filter { it.isSelected }
        if (selected.isNotEmpty()) {
            selected.forEach { onItemLongClick(categories.indexOf(it)) }
        }
    }

    /**
     * Show MaterialDialog which let user change category name.
     *
     * @param category category that will be edited.
     */
    private fun editCategory(category: Category) {
        MaterialDialog.Builder(this)
                .title(R.string.action_rename_category)
                .negativeText(android.R.string.cancel)
                .input(getString(R.string.name), category.name, false)
                { dialog, input -> presenter.renameCategory(category, input.toString()) }
                .show()
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
                        .remove(adapter.selectedPositions, recycler.parent as View,
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
        adapter.mode = FlexibleAdapter.MODE_MULTI
        return true
    }

    /**
     * Called each time the action mode is shown.
     * Always called after onCreateActionMode
     *
     * @return false
     */
    override fun onPrepareActionMode(actionMode: ActionMode, menu: Menu): Boolean {
        val count = adapter.selectedItemCount
        actionMode.title = getString(R.string.label_selected, count)

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
        adapter.mode = FlexibleAdapter.MODE_IDLE
        adapter.clearSelection()
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
        // Check if action mode is initialized.
        if (actionMode == null) {
            // Initialize action mode
            actionMode = startSupportActionMode(this)
        }

        // Set item as selected
        toggleSelection(position)
    }

    /**
     * Toggle the selection state of an item.
     * If the item was the last one in the selection and is unselected, the ActionMode is finished.
     */
    private fun toggleSelection(position: Int) {
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
        val categories = (0..adapter.itemCount-1).map { adapter.getItem(it).category }
        presenter.reorderCategories(categories)
    }

    /**
     * Called when the undo action is clicked in the snackbar.
     */
    override fun onUndoConfirmed(action: Int) {
        adapter.restoreDeletedItems()
    }

    /**
     * Called when the time to restore the items expires.
     */
    override fun onDeleteConfirmed(action: Int) {
        presenter.deleteCategories(adapter.deletedItems.map { it.category })
    }

}