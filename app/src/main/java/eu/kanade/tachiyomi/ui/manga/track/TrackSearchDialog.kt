package eu.kanade.tachiyomi.ui.manga.track

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.databinding.TrackSearchDialogBinding
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.manga.MangaController
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import reactivecircus.flowbinding.android.widget.textChanges
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit

class TrackSearchDialog : DialogController {

    private var binding: TrackSearchDialogBinding? = null

    private var adapter: TrackSearchAdapter? = null

    private val service: TrackService
    private val currentTrackUrl: String?

    private val trackController
        get() = targetController as MangaController

    constructor(target: MangaController, service: TrackService, currentTrackUrl: String?) : super(
        bundleOf(KEY_SERVICE to service.id, KEY_CURRENT_URL to currentTrackUrl)
    ) {
        targetController = target
        this.service = service
        this.currentTrackUrl = currentTrackUrl
    }

    @Suppress("unused")
    constructor(bundle: Bundle) : super(bundle) {
        service = Injekt.get<TrackManager>().getService(bundle.getInt(KEY_SERVICE))!!
        currentTrackUrl = bundle.getString(KEY_CURRENT_URL)
    }

    @Suppress("DEPRECATION")
    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        binding = TrackSearchDialogBinding.inflate(LayoutInflater.from(activity!!))
        val dialog = MaterialDialog(activity!!)
            .customView(view = binding!!.root)
            .positiveButton(android.R.string.ok) { onPositiveButtonClick() }
            .negativeButton(android.R.string.cancel)

        if (currentTrackUrl != null) {
            dialog.neutralButton(R.string.action_remove) { onRemoveButtonClick() }
        }

        onViewCreated(dialog.view, savedViewState)

        return dialog
    }

    fun onViewCreated(view: View, savedState: Bundle?) {
        // Create adapter
        adapter = TrackSearchAdapter(currentTrackUrl)
        binding!!.trackSearchRecyclerview.layoutManager = LinearLayoutManager(view.context)
        binding!!.trackSearchRecyclerview.adapter = adapter

        // Do an initial search based on the manga's title
        if (savedState == null) {
            val title = trackController.presenter.manga.title
            binding!!.trackSearch.append(title)
            search(title)
        }
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        binding = null
        adapter = null
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        binding!!.trackSearch.textChanges()
            .debounce(TimeUnit.SECONDS.toMillis(1))
            .filter { it.isNotBlank() }
            .onEach { search(it.toString()) }
            .launchIn(trackController.viewScope)
    }

    private fun search(query: String) {
        val binding = binding ?: return
        binding.progress.isVisible = true
        binding.trackSearchRecyclerview.isVisible = false
        trackController.presenter.trackingSearch(query, service)
    }

    fun onSearchResults(results: List<TrackSearch>) {
        val binding = binding ?: return
        binding.progress.isVisible = false
        binding.trackSearchRecyclerview.isVisible = true
        adapter?.items = results
    }

    fun onSearchResultsError() {
        val binding = binding ?: return
        binding.progress.isVisible = false
        binding.trackSearchRecyclerview.isVisible = false
        adapter?.items = emptyList()
    }

    private fun onPositiveButtonClick() {
        val adapter = adapter ?: return
        val item = adapter.items.getOrNull(adapter.selectedItemPosition)
        if (item != null) {
            trackController.presenter.registerTracking(item, service)
        }
    }

    private fun onRemoveButtonClick() {
        trackController.presenter.unregisterTracking(service)
    }

    private companion object {
        const val KEY_SERVICE = "service_id"
        const val KEY_CURRENT_URL = "current_url"
    }
}
