package eu.kanade.tachiyomi.ui.base.controller

import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import androidx.appcompat.widget.SearchView
import androidx.viewbinding.ViewBinding
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import reactivecircus.flowbinding.appcompat.QueryTextEvent
import reactivecircus.flowbinding.appcompat.queryTextEvents

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

    /**
     * To be called by classes that extend this subclass in onCreateOptionsMenu
     */
    protected fun commonCreateOptionsMenu(
        menu: Menu,
        inflater: MenuInflater,
        menuId: Int,
        searchItemId: Int,
        queryHint: String = "",
        restoreCurrentQuery: Boolean = true
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
        } else {
            if (queryHint.isNotBlank()) {
                searchView.queryHint = queryHint
            }

            if (restoreCurrentQuery) {
                val query = presenter.query

                // Restoring a query the user had submitted
                if (query.isNotBlank()) {
                    searchItem.expandActionView()
                    searchView.setQuery(query, true)
                    searchView.clearFocus()
                }
            }
        }

        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            storeNonSubmittedQuery = hasFocus
        }

        searchView.queryTextEvents()
            .debounce(300) // prevent overloading the controller as user types
            .onEach {
                val newText = it.queryText.toString()

                if (it is QueryTextEvent.QuerySubmitted) {
                    // Abstract function for implementation
                    // Run it first in case the old query data is needed (like BrowseSourceController)
                    onSearchViewQueryTextSubmit(newText)
                    presenter.query = newText
                    nonSubmittedQuery = ""
                } else if ((it is QueryTextEvent.QueryChanged) && (presenter.query != newText)) {
                    // Ignore events triggered when the search is not in focus
                    if (storeNonSubmittedQuery) {
                        nonSubmittedQuery = newText
                    }

                    // Abstract function for implementation
                    onSearchViewQueryTextChange(newText)
                }
            }
            .launchIn(viewScope)
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
     * Not abstract as they are optional
     */
    protected open fun onSearchViewQueryTextChange(newText: String?) {
    }

    protected open fun onSearchViewQueryTextSubmit(query: String?) {
    }
}
