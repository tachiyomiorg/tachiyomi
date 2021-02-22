package eu.kanade.tachiyomi.ui.base.controller

import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
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

    enum class SearchViewState(val state: Int) {
        LOADING(0),
        LOADED(1),
        COLLAPSING(2)
    }

    /**
     * Bool used to bypass the initial searchView being set to empty string after an onResume
     */
    private var acceptEmptyQuery: Boolean = false
    private var isLoaded: Boolean = false

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
        searchItem.fixExpand(onExpand = { invalidateMenuOnExpand() })
        searchView.maxWidth = Int.MAX_VALUE

        searchView.queryTextEvents()
            .onEach {
                val newText = it.queryText.toString()

                if (newText.isNotBlank() or acceptEmptyQuery) {
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
            }
            .launchIn(viewScope)

        val query = presenter.query

        // Restoring a query the user had not submitted
        if (nonSubmittedQuery.isNotBlank() and (nonSubmittedQuery != query)) {
            searchItem.expandActionView()
            searchView.setQuery(nonSubmittedQuery, false)
            onSearchViewQueryTextChange(nonSubmittedQuery)
        } else {
            if (queryHint.isNotBlank()) {
                searchView.queryHint = queryHint
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
        searchView.postDelayed(
            {
                isLoaded = true
                setAcceptEmptyQuery(true)
            },
            0
        )

        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            setAcceptEmptyQuery(hasFocus)
        }
    }

    private fun setAcceptEmptyQuery(newValue: Boolean) {
        acceptEmptyQuery = newValue && isLoaded
    }

    override fun onActivityResumed(activity: Activity) {
        super.onActivityResumed(activity)
        // Until everything is up and running don't accept empty queries
        isLoaded = false
        setAcceptEmptyQuery(false)
    }

    /**
     * Called by the SearchView since since the implementation of these can vary in subclasses
     * Not abstract as they are optional
     */
    protected open fun onSearchViewQueryTextChange(newText: String?) {
    }

    protected open fun onSearchViewQueryTextSubmit(query: String?) {
    }

    /**
     * During the conversion to SearchableNucleusController (after which I plan to merge its code
     * into BaseController) this addresses an issue where the searchView.onTextFocus event is not
     * triggered
     */
    override fun invalidateMenuOnExpand(): Boolean {
        return if (expandActionViewFromInteraction) {
            activity?.invalidateOptionsMenu()
            setAcceptEmptyQuery(true) // we are technically focused here
            false
        } else {
            true
        }
    }
}
