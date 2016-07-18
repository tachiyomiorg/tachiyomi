package eu.kanade.tachiyomi.ui.library

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager

import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.ui.base.adapter.SmartFragmentStatePagerAdapter

/**
 * This adapter stores the categories from the library, used with a ViewPager.
 *
 * @param fm the fragment manager.
 * @constructor creates an instance of the adapter.
 */
class LibraryAdapter(fm: FragmentManager) : SmartFragmentStatePagerAdapter(fm) {

    /**
     * The categories to bind in the adapter.
     */
    var categories: List<Category>? = null
        // This setter helps to not refresh the adapter if the reference to the list doesn't change.
        set(value) {
            if (field !== value) {
                field = value
                notifyDataSetChanged()
            }
        }

    /**
     * Creates a new fragment for the given position when it's called.
     *
     * @param position the position to instantiate.
     * @return a fragment for the given position.
     */
    override fun getItem(position: Int): Fragment {
        return LibraryCategoryFragment.newInstance(position)
    }

    /**
     * Returns the number of categories.
     *
     * @return the number of categories or 0 if the list is null.
     */
    override fun getCount(): Int {
        return categories?.size ?: 0
    }

    /**
     * Returns the title to display for a category.
     *
     * @param position the position of the element.
     * @return the title to display.
     */
    override fun getPageTitle(position: Int): CharSequence {
        return categories!![position].name
    }

    /**
     * Method to enable or disable the action mode (multiple selection) for all the instantiated
     * fragments.
     *
     * @param mode the mode to set.
     */
    fun setSelectionMode(mode: Int) {
        for (fragment in getRegisteredFragments()) {
            (fragment as LibraryCategoryFragment).setSelectionMode(mode)
        }
    }

    /**
     * Notifies the adapters in all the registered fragments to refresh their content.
     */
    fun refreshRegisteredAdapters() {
        for (fragment in getRegisteredFragments()) {
            (fragment as LibraryCategoryFragment).adapter.notifyDataSetChanged()
        }
    }

}