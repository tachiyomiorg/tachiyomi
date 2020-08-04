package eu.kanade.tachiyomi.ui.manga.track

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import java.util.concurrent.TimeUnit
import kotlinx.android.synthetic.main.track_search_dialog.view.progress
import kotlinx.android.synthetic.main.track_search_dialog.view.track_search
import kotlinx.android.synthetic.main.track_search_dialog.view.track_search_list
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import reactivecircus.flowbinding.android.widget.itemClicks
import reactivecircus.flowbinding.android.widget.textChanges
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class TrackSearchDialog : DialogController {

    private var dialogView: View? = null

    private var adapter: TrackSearchAdapter? = null

    private var selectedItem: Track? = null

    private val service: TrackService

    private val trackController
        get() = targetController as TrackController

    constructor(target: TrackController, service: TrackService) : super(
        Bundle().apply {
            putInt(KEY_SERVICE, service.id)
        }
    ) {
        targetController = target
        this.service = service
    }

    @Suppress("unused")
    constructor(bundle: Bundle) : super(bundle) {
        service = Injekt.get<TrackManager>().getService(bundle.getInt(KEY_SERVICE))!!
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        val dialog = MaterialDialog(activity!!)
            .customView(R.layout.track_search_dialog)
            .positiveButton(android.R.string.ok) { onPositiveButtonClick() }
            .negativeButton(android.R.string.cancel)
            .neutralButton(R.string.action_remove) { onRemoveButtonClick() }

        dialogView = dialog.view
        onViewCreated(dialog.view, savedViewState)

        return dialog
    }

    fun onViewCreated(view: View, savedState: Bundle?) {
        // Create adapter
        val adapter = TrackSearchAdapter(view.context)
        this.adapter = adapter
        view.track_search_list.adapter = adapter

        // Set listeners
        selectedItem = null

        view.track_search_list.itemClicks()
            .onEach { position ->
                selectedItem = adapter.getItem(position)
            }
            .launchIn(trackController.scope)

        // Do an initial search based on the manga's title
        if (savedState == null) {
            val title = trackController.presenter.manga.title
            view.track_search.append(title)
            search(title)
        }
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        dialogView = null
        adapter = null
    }

    @FlowPreview
    override fun onAttach(view: View) {
        super.onAttach(view)
        dialogView!!.track_search.textChanges()
            .debounce(TimeUnit.SECONDS.toMillis(1))
            .filter { it.isNotBlank() }
            .onEach { search(it.toString()) }
            .launchIn(trackController.scope)
    }

    private fun search(query: String) {
        val view = dialogView ?: return
        view.progress.isVisible = true
        view.track_search_list.isInvisible = true
        trackController.presenter.search(query, service)
    }

    fun onSearchResults(results: List<TrackSearch>) {
        selectedItem = null
        val view = dialogView ?: return
        view.progress.isInvisible = true
        view.track_search_list.isVisible = true
        adapter?.setItems(results)
    }

    fun onSearchResultsError() {
        val view = dialogView ?: return
        view.progress.isVisible = true
        view.track_search_list.isInvisible = true
        adapter?.setItems(emptyList())
    }

    private fun onPositiveButtonClick() {
        trackController.presenter.registerTracking(selectedItem, service)
    }

    private fun onRemoveButtonClick() {
        trackController.presenter.unregisterTracking(service)
    }

    private companion object {
        const val KEY_SERVICE = "service_id"
    }
}
