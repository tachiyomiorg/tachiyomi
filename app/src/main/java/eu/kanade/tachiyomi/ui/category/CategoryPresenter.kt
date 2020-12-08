package eu.kanade.tachiyomi.ui.category

import android.os.Bundle
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Presenter of [CategoryController]. Used to manage the categories of the library.
 */
class CategoryPresenter(
    private val db: DatabaseHelper = Injekt.get()
) : BasePresenter<CategoryController>() {

    /**
     * List containing categories.
     */
    private var categories: List<Category> = emptyList()

    /**
     * Called when the presenter is created.
     *
     * @param savedState The saved state of this presenter.
     */
    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        db.getCategories().asRxObservable()
            .doOnNext { categories = it }
            .map { it.map(::CategoryItem) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeLatestCache(CategoryController::setCategories)
    }

    /**
     * Creates and adds a new category to the database.
     *
     * @param name The name of the category to create.
     */
    fun createCategory(name: String) {
        // Do not allow duplicate categories.
        if (categoryExists(name)) {
            Observable.just(Unit).subscribeFirst({ view, _ -> view.onCategoryExistsError() })
            return
        }

        // Create category.
        val cat = Category.create(name)

        // Set the new item in the last position.
        cat.order = categories.map { it.order + 1 }.maxOrNull() ?: 0

        // Insert into database.
        db.insertCategory(cat).asRxObservable().subscribe()
    }

    /**
     * Deletes the given categories from the database.
     *
     * @param categories The list of categories to delete.
     */
    fun deleteCategories(categories: List<Category>) {
        db.deleteCategories(categories).asRxObservable().subscribe()
    }

    /**
     * Reorders the given categories in the database.
     *
     * @param categories The list of categories to reorder.
     */
    fun reorderCategories(categories: List<Category>) {
        categories.forEachIndexed { i, category ->
            category.order = i
        }

        db.insertCategories(categories).asRxObservable().subscribe()
    }

    /**
     * Renames a category.
     *
     * @param category The category to rename.
     * @param name The new name of the category.
     */
    fun renameCategory(category: Category, name: String) {
        // Do not allow duplicate categories.
        if (categoryExists(name)) {
            Observable.just(Unit).subscribeFirst({ view, _ -> view.onCategoryExistsError() })
            return
        }

        category.name = name
        db.insertCategory(category).asRxObservable().subscribe()
    }

    /**
     * Returns true if a category with the given name already exists.
     */
    private fun categoryExists(name: String): Boolean {
        return categories.any { it.name == name }
    }
}
