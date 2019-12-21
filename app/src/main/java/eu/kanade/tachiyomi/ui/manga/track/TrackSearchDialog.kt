package eu.kanade.tachiyomi.ui.manga.track

import android.app.Dialog
import android.os.Bundle
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import com.jakewharton.rxbinding.widget.itemClicks
import com.jakewharton.rxbinding.widget.textChanges
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.util.plusAssign
import kotlinx.android.synthetic.main.track_search_dialog.view.*
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit

class TrackSearchDialog : DialogController {

    private var dialogView: View? = null

    private var adapter: TrackSearchAdapter? = null

    private var selectedItem: Track? = null

    private val service: TrackService

    private var subscriptions = CompositeSubscription()

    private var searchTextSubscription: Subscription? = null

    private val trackController
        get() = targetController as TrackController

    constructor(target: TrackController, service: TrackService) : super(Bundle().apply {
        putInt(KEY_SERVICE, service.id)
    }) {
        targetController = target
        this.service = service
    }

    @Suppress("unused")
    constructor(bundle: Bundle) : super(bundle) {
        service = Injekt.get<TrackManager>().getService(bundle.getInt(KEY_SERVICE))!!
    }

    override fun onCreateDialog(savedState: Bundle?): Dialog {
        val dialog = MaterialDialog.Builder(activity!!)
                .customView(R.layout.track_search_dialog, false)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .onPositive { _, _ -> onPositiveButtonClick() }
                .build()

        if (subscriptions.isUnsubscribed) {
            subscriptions = CompositeSubscription()
        }

        dialogView = dialog.view
        onViewCreated(dialog.view, savedState)

        return dialog
    }

    fun onViewCreated(view: View, savedState: Bundle?) {
        // Create adapter
        val adapter = TrackSearchAdapter(view.context)
        this.adapter = adapter
        view.track_search_list.adapter = adapter

        // Set listeners
        selectedItem = null

        subscriptions += view.track_search_list.itemClicks().subscribe { position ->
            selectedItem = adapter.getItem(position)
        }

        // Do an initial search based on the manga's title
        if (savedState == null) {
            val title = trackController.presenter.manga.title
            view.track_search.append(title)
            search(title)
        }
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        subscriptions.unsubscribe()
        dialogView = null
        adapter = null
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        searchTextSubscription = dialogView!!.track_search.textChanges()
                .skip(1)
                .debounce(1, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                .map { it.toString() }
                .filter(String::isNotBlank)
                .subscribe { search(it) }
    }

    override fun onDetach(view: View) {
        super.onDetach(view)
        searchTextSubscription?.unsubscribe()
    }

    private fun search(query: String) {
        val view = dialogView ?: return
        view.progress.visibility = View.VISIBLE
        view.track_search_list.visibility = View.INVISIBLE
        trackController.presenter.search(query, service)
    }

    fun onSearchResults(results: List<TrackSearch>) {
        selectedItem = null
        val view = dialogView ?: return
        view.progress.visibility = View.INVISIBLE
        view.track_search_list.visibility = View.VISIBLE
        adapter?.setItems(results)
    }

    fun onSearchResultsError() {
        val view = dialogView ?: return
        view.progress.visibility = View.VISIBLE
        view.track_search_list.visibility = View.INVISIBLE
        adapter?.setItems(emptyList())
    }

    private fun onPositiveButtonClick() {
        trackController.presenter.registerTracking(selectedItem, service)
    }

    private companion object {
        const val KEY_SERVICE = "service_id"
    }

}
