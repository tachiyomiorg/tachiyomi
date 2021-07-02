package eu.kanade.tachiyomi.ui.base.controller

import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.annotation.StringRes
import androidx.appcompat.widget.SearchView
import androidx.viewbinding.ViewBinding
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import reactivecircus.flowbinding.appcompat.QueryTextEvent
import reactivecircus.flowbinding.appcompat.queryTextEvents

/**
 * Implementation of the NucleusController that has a built-in ViewSearch
 */
abstract class SearchableNucleusController<VB : ViewBinding, P : BasePresenter<*>>
(bundle: Bundle? = null) : NucleusController<VB, P>(bundle) {

    enum class SearchViewState { LOADING, LOADED, COLLAPSING, FOCUSED }

    /**
     * Used to bypass the initial searchView being set to empty string after an onResume
     */
    private var currentSearchViewState: SearchViewState = SearchViewState.LOADING

    /**
     * Store the query text that has not been submitted to reassign it after an onResume, UI-only
     */
    protected var nonSubmittedQuery: String = ""

    /**
     * To be called by classes that extend this subclass in onCreateOptionsMenu
     */
    protected fun createOptionsMenu(
        menu: Menu,
        inflater: MenuInflater,
        menuId: Int,
        searchItemId: Int,
        @StringRes queryHint: Int? = null,
        restoreCurrentQuery: Boolean = true
    ) {
        // Inflate menu
        inflater.inflate(menuId, menu)

        // Initialize search option.
        val searchItem = menu.findItem(searchItemId)
        val searchView = searchItem.actionView as SearchView
        searchItem.fixExpand(onExpand = { invalidateMenuOnExpand() })
        searchView.maxWidth = Int.MAX_VALUE

        searchView.queryTextEvents()
            .onEach {
                val newText = it.queryText.toString()

                if (newText.isNotBlank() or acceptEmptyQuery()) {
                    if (it is QueryTextEvent.QuerySubmitted) {
                        // Abstract function for implementation
                        // Run it first in case the old query data is needed (like BrowseSourceController)
                        onSearchViewQueryTextSubmit(newText)
                        presenter.query = newText
                        nonSubmittedQuery = ""
                    } else if ((it is QueryTextEvent.QueryChanged) && (presenter.query != newText)) {
                        nonSubmittedQuery = newText

                        // Abstract function for implementation
                        onSearchViewQueryTextChange(newText)
                    }
                }
                // clear the collapsing flag
                setCurrentSearchViewState(SearchViewState.LOADED, SearchViewState.COLLAPSING)
            }
            .launchIn(viewScope)

        val query = presenter.query

        // Restoring a query the user had not submitted
        if (nonSubmittedQuery.isNotBlank() and (nonSubmittedQuery != query)) {
            searchItem.expandActionView()
            searchView.setQuery(nonSubmittedQuery, false)
            onSearchViewQueryTextChange(nonSubmittedQuery)
        } else {
            if (queryHint != null) {
                searchView.queryHint = applicationContext?.getString(queryHint)
            }

            if (restoreCurrentQuery) {
                // Restoring a query the user had submitted
                if (query.isNotBlank()) {
                    searchItem.expandActionView()
                    searchView.setQuery(query, true)
                    searchView.clearFocus()
                    onSearchViewQueryTextChange(query)
                    onSearchViewQueryTextSubmit(query)
                }
            }
        }

        // Workaround for weird behavior where searchView gets empty text change despite
        // query being set already, prevents the query from being cleared
        binding.root.post {
            setCurrentSearchViewState(SearchViewState.LOADED, SearchViewState.LOADING)
        }

        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                setCurrentSearchViewState(SearchViewState.FOCUSED)
            } else {
                setCurrentSearchViewState(SearchViewState.LOADED, SearchViewState.FOCUSED)
            }
        }

        searchItem.setOnActionExpandListener(
            object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                    onSearchMenuItemActionExpand(item)
                    return true
                }

                override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                    val localSearchView = searchItem.actionView as SearchView

                    // if it is blank the flow event won't trigger so we would stay in a COLLAPSING state
                    if (localSearchView.toString().isNotBlank()) {
                        setCurrentSearchViewState(SearchViewState.COLLAPSING)
                    }

                    onSearchMenuItemActionCollapse(item)
                    return true
                }
            }
        )
    }

    override fun onActivityResumed(activity: Activity) {
        super.onActivityResumed(activity)
        // Until everything is up and running don't accept empty queries
        setCurrentSearchViewState(SearchViewState.LOADING)
    }

    private fun acceptEmptyQuery(): Boolean {
        return when (currentSearchViewState) {
            SearchViewState.COLLAPSING, SearchViewState.FOCUSED -> true
            else -> false
        }
    }

    private fun setCurrentSearchViewState(to: SearchViewState, from: SearchViewState? = null) {
        // When loading ignore all requests other than loaded
        if ((currentSearchViewState == SearchViewState.LOADING) && (to != SearchViewState.LOADED)) {
            return
        }

        // Prevent changing back to an unwanted state when using async flows (ie onFocus event doing
        // COLLAPSING -> LOADED)
        if ((from != null) && (currentSearchViewState != from)) {
            return
        }

        currentSearchViewState = to
    }

    /**
     * Called by the SearchView since since the implementation of these can vary in subclasses
     * Not abstract as they are optional
     */
    protected open fun onSearchViewQueryTextChange(newText: String?) {
    }

    protected open fun onSearchViewQueryTextSubmit(query: String?) {
    }

    protected open fun onSearchMenuItemActionExpand(item: MenuItem?) {
    }

    protected open fun onSearchMenuItemActionCollapse(item: MenuItem?) {
    }

    /**
     * During the conversion to SearchableNucleusController (after which I plan to merge its code
     * into BaseController) this addresses an issue where the searchView.onTextFocus event is not
     * triggered
     */
    override fun invalidateMenuOnExpand(): Boolean {
        return if (expandActionViewFromInteraction) {
            activity?.invalidateOptionsMenu()
            setCurrentSearchViewState(SearchViewState.FOCUSED) // we are technically focused here
            false
        } else {
            true
        }
    }
}
