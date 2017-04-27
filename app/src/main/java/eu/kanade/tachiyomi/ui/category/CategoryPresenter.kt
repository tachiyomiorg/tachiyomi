package eu.kanade.tachiyomi.ui.category

import android.os.Bundle
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.util.toast
import rx.android.schedulers.AndroidSchedulers
import uy.kohesive.injekt.injectLazy

/**
 * Presenter of CategoryActivity.
 * Contains information and data for activity.
 * Observable updates should be called from here.
 */
class CategoryPresenter : BasePresenter<CategoryActivity>() {

    /**
     * Used to connect to database.
     */
    private val db: DatabaseHelper by injectLazy()

    /**
     * List containing categories.
     */
    private var categories: List<Category> = emptyList()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        db.getCategories().asRxObservable()
                .doOnNext { categories = it }
                .map { it.map(::CategoryItem) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeLatestCache(CategoryActivity::setCategories)
    }

    /**
     * Create category and add it to database
     *
     * @param name name of category
     */
    fun createCategory(name: String) {
        // Do not allow duplicate categories.
        if (categories.any { it.name.equals(name, true) }) {
            context.toast(R.string.error_category_exists)
            return
        }

        // Create category.
        val cat = Category.create(name)

        // Set the new item in the last position.
        cat.order = categories.map { it.order + 1 }.max() ?: 0

        // Insert into database.
        db.insertCategory(cat).asRxObservable().subscribe()
    }

    /**
     * Delete category from database
     *
     * @param categories list of categories
     */
    fun deleteCategories(categories: List<Category>) {
        db.deleteCategories(categories).asRxObservable().subscribe()
    }

    /**
     * Reorder categories in database
     *
     * @param categories list of categories
     */
    fun reorderCategories(categories: List<Category>) {
        categories.forEachIndexed { i, category ->
            category.order = i
        }

        db.insertCategories(categories).asRxObservable().subscribe()
    }

    /**
     * Rename a category
     *
     * @param category category that gets renamed
     * @param name new name of category
     */
    fun renameCategory(category: Category, name: String) {
        // Do not allow duplicate categories.
        if (categories.any { it.name.equals(name, true) }) {
            context.toast(R.string.error_category_exists)
            return
        }

        category.name = name
        db.insertCategory(category).asRxObservable().subscribe()
    }
}