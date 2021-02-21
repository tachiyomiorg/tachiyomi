package eu.kanade.tachiyomi.ui.base.controller

import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import androidx.appcompat.widget.SearchView
import androidx.viewbinding.ViewBinding
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter

/**
 * Implementation of the NucleusController that has a built-in ViewSearch
 */
abstract class SearchableNucleusController<VB : ViewBinding, P : BasePresenter<*>>
(bundle: Bundle? = null) : NucleusController<VB, P>(bundle) {

    /**
     * Bool used to bypass the initial searchView being set to empty string after an onResume
     */
    protected var storeNonSubmittedQuery: Boolean = false

    /**
     * Store the query text that has not been submitted to reassign it after an onResume, UI-only
     */
    protected var nonSubmittedQuery: String = ""

    protected fun commonCreateOptionsMenu(
        menu: Menu,
        inflater: MenuInflater,
        menuId: Int,
        searchItemId: Int,
        useGlobalSearch: Boolean = false
    ) {
        // Inflate menu
        inflater.inflate(menuId, menu)

        // Initialize search option.
        val searchItem = menu.findItem(searchItemId)
        val searchView = searchItem.actionView as SearchView
        searchView.maxWidth = Int.MAX_VALUE

        // Restoring a query the user had not submitted
        if (nonSubmittedQuery.isNotBlank()) {
            searchItem.expandActionView()
            searchView.setQuery(nonSubmittedQuery, false)
            storeNonSubmittedQuery = true // searchView.requestFocus() does not seem to work here
        } else if (useGlobalSearch) {
            // Change hint to show global search.
            searchView.queryHint = applicationContext?.getString(R.string.action_global_search_hint)
        } else {
            val query = presenter.query

            // Restoring a query the user had submitted
            if (query.isNotBlank()) {
                searchItem.expandActionView()
                searchView.setQuery(query, true)
                searchView.clearFocus()
            }
        }

        initSearchHandler(searchView)
    }

    private fun initSearchHandler(searchView: SearchView) {
        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            storeNonSubmittedQuery = hasFocus
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            // Save the query string whenever it changes to be able to store it for persistence
            override fun onQueryTextChange(newText: String?): Boolean {
                // Ignore events triggered when the search is not in focus
                if (storeNonSubmittedQuery) {
                    nonSubmittedQuery = newText ?: ""
                }
                // Abstract function for implementation
                onSearchViewQueryTextChange(newText)
                return false
            }

            // Only perform search when the query is submitted
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Abstract function for implementation
                // Run it first in case the old query data is needed (like BrowseSourceController)
                onSearchViewQueryTextSubmit(query)
                presenter.query = query ?: ""
                nonSubmittedQuery = ""
                return true
            }
        })
    }

    override fun onActivityResumed(activity: Activity) {
        super.onActivityResumed(activity)
        // searchView.onQueryTextChange is triggered after this, and the query set to "", so we make
        // sure not to save it (onActivityResumed --> onQueryTextChange
        // --> OnQueryTextFocusChangeListener --> onCreateOptionsMenu)
        storeNonSubmittedQuery = false
    }

    /**
     * Called by the SearchView since since the implementation of these can vary in subclasses
     */
    protected abstract fun onSearchViewQueryTextChange(newText: String?)
    protected abstract fun onSearchViewQueryTextSubmit(query: String?)
}
