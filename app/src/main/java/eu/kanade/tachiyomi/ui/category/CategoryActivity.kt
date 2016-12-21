package eu.kanade.tachiyomi.ui.category

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.view.ActionMode
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.Menu
import android.view.MenuItem
import com.afollestad.materialdialogs.MaterialDialog
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.ui.base.activity.BaseRxActivity
import eu.kanade.tachiyomi.ui.base.adapter.FlexibleViewHolder
import eu.kanade.tachiyomi.ui.base.adapter.OnStartDragListener
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
        ActionMode.Callback, FlexibleViewHolder.OnListItemClickListener, OnStartDragListener {

    /**
     * Object used to show actionMode toolbar.
     */
    var actionMode: ActionMode? = null

    /**
     * Adapter containing category items.
     */
    private lateinit var adapter: CategoryAdapter

    /**
     * TouchHelper used for reorder animation and movement.
     */
    private lateinit var touchHelper: ItemTouchHelper

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

        // Touch helper to drag and reorder categories
        touchHelper = ItemTouchHelper(CategoryItemTouchHelper(adapter))
        touchHelper.attachToRecyclerView(recycler)

        // Create OnClickListener for creating new category
        fab.setOnClickListener({ v ->
            MaterialDialog.Builder(this)
                    .title(R.string.action_add_category)
                    .negativeText(android.R.string.cancel)
                    .input(R.string.name, 0, false)
                    { dialog, input -> presenter.createCategory(input.toString()) }
                    .show()
        })
    }

    /**
     * Finishes action mode.
     * Call this when action mode action is finished.
     */
    fun destroyActionModeIfNeeded() {
        actionMode?.finish()
    }

    /**
     * Fill adapter with category items
     *
     * @param categories list containing categories
     */
    fun setCategories(categories: List<Category>) {
        destroyActionModeIfNeeded()
        adapter.setItems(categories)
    }

    /**
     * Returns the selected categories
     *
     * @return list of selected categories
     */
    private fun getSelectedCategories(): List<Category> {
        // Create a list of the selected categories
        return adapter.selectedItems.map { adapter.getItem(it) }
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
                .onNegative { materialDialog, dialogAction -> destroyActionModeIfNeeded() }
                .input(getString(R.string.name), category.name, false)
                { dialog, input -> presenter.renameCategory(category, input.toString()) }
                .show()
    }

    /**
     * Toggle actionMode selection
     *
     * @param position position of selected item
     */
    private fun toggleSelection(position: Int) {
        adapter.toggleSelection(position, false)

        // Get selected item count
        val count = adapter.selectedItemCount

        // If no item is selected finish action mode
        if (count == 0) {
            actionMode?.finish()
        } else {
            // This block will only run if actionMode is not null
            actionMode?.let {

                // Set title equal to selected item
                it.title = getString(R.string.label_selected, count)
                it.invalidate()

                // Show edit button only when one item is selected
                val editItem = it.menu.findItem(R.id.action_edit)
                editItem.isVisible = count == 1
            }
        }
    }

    /**
     * Called each time the action mode is shown.
     * Always called after onCreateActionMode
     *
     * @return false
     */
    override fun onPrepareActionMode(actionMode: ActionMode, menu: Menu): Boolean {
        return false
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
                // Delete select categories.
                presenter.deleteCategories(getSelectedCategories())
            }
            R.id.action_edit -> {
                // Edit selected category
                editCategory(getSelectedCategories()[0])
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
     * Called when action mode destroyed.
     *
     * @param mode ActionMode object.
     */
    override fun onDestroyActionMode(mode: ActionMode?) {
        // Reset adapter to single selection
        adapter.mode = FlexibleAdapter.MODE_SINGLE
        // Clear selected items
        adapter.clearSelection()
        actionMode = null
    }

    /**
     * Called when item in list is clicked.
     *
     * @param position position of clicked item.
     */
    override fun onListItemClick(position: Int): Boolean {
        // Check if action mode is initialized and selected item exist.
        if (position == -1) {
            return false
        } else if (actionMode != null) {
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
    override fun onListItemLongClick(position: Int) {
        // Check if action mode is initialized.
        if (actionMode == null)
        // Initialize action mode
            actionMode = startSupportActionMode(this)

        // Set item as selected
        toggleSelection(position)
    }

    /**
     * Called when item is dragged
     *
     * @param viewHolder view that contains dragged item
     */
    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        // Notify touchHelper
        touchHelper.startDrag(viewHolder)
    }

}