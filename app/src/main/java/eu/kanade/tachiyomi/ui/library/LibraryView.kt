package eu.kanade.tachiyomi.ui.library

import android.annotation.SuppressLint
import android.support.v4.view.ViewPager
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Category

@SuppressLint("ViewConstructor")
class LibraryView(private val controller: LibraryController) : ViewPager(controller.activity) {

    private var lAdapter = LibraryAdapter(controller)

    init {
        id = R.id.view_pager
        layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
        adapter = lAdapter
    }

    fun setCategories(categories: List<Category>) {
        // Get the current active category.
        val activeCat = if (lAdapter.categories.isNotEmpty())
            currentItem
        else
            controller.activeCategory

        // Set the categories
        lAdapter.categories = categories

        // Restore active category.
        setCurrentItem(activeCat, false)
    }

    fun reattachAdapter() {
        val position = currentItem
        lAdapter.recycle = false
        adapter = lAdapter
        currentItem = position
        lAdapter.recycle = true
    }
}