package eu.kanade.tachiyomi.ui.manga.track

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.databinding.TrackControllerBinding
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.manga.MangaController
import eu.kanade.tachiyomi.util.lang.launchInUI
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.onEach
import reactivecircus.flowbinding.swiperefreshlayout.refreshes
import timber.log.Timber

class TrackController : NucleusController<TrackControllerBinding, TrackPresenter>(),
        TrackAdapter.OnClickListener,
        SetTrackStatusDialog.Listener,
        SetTrackChaptersDialog.Listener,
        SetTrackScoreDialog.Listener {

    private var adapter: TrackAdapter? = null

    init {
        // There's no menu, but this avoids a bug when coming from the catalogue, where the menu
        // disappears if the searchview is expanded
        setHasOptionsMenu(true)
    }

    override fun createPresenter(): TrackPresenter {
        return TrackPresenter((parentController as MangaController).manga!!)
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        binding = TrackControllerBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        adapter = TrackAdapter(this)
        binding.trackRecycler.layoutManager = LinearLayoutManager(view.context)
        binding.trackRecycler.adapter = adapter
        binding.swipeRefresh.isEnabled = false
        binding.swipeRefresh.refreshes()
            .onEach { presenter.refresh() }
            .launchInUI()
    }

    override fun onDestroyView(view: View) {
        adapter = null
        super.onDestroyView(view)
    }

    fun onNextTrackings(trackings: List<TrackItem>) {
        val atLeastOneLink = trackings.any { it.track != null }
        adapter?.items = trackings
        binding.swipeRefresh.isEnabled = atLeastOneLink
        (parentController as? MangaController)?.setTrackingIcon(atLeastOneLink)
    }

    fun onSearchResults(results: List<TrackSearch>) {
        getSearchDialog()?.onSearchResults(results)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onSearchResultsError(error: Throwable) {
        Timber.e(error)
        getSearchDialog()?.onSearchResultsError()
    }

    private fun getSearchDialog(): TrackSearchDialog? {
        return router.getControllerWithTag(TAG_SEARCH_CONTROLLER) as? TrackSearchDialog
    }

    fun onRefreshDone() {
        binding.swipeRefresh.isRefreshing = false
    }

    fun onRefreshError(error: Throwable) {
        binding.swipeRefresh.isRefreshing = false
        activity?.toast(error.message)
    }

    override fun onLogoClick(position: Int) {
        val track = adapter?.getItem(position)?.track ?: return

        if (track.tracking_url.isBlank()) {
            activity?.toast(R.string.url_not_set)
        } else {
            activity?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(track.tracking_url)))
        }
    }

    override fun onSetClick(position: Int) {
        val item = adapter?.getItem(position) ?: return
        TrackSearchDialog(this, item.service).showDialog(router, TAG_SEARCH_CONTROLLER)
    }

    override fun onStatusClick(position: Int) {
        val item = adapter?.getItem(position) ?: return
        if (item.track == null) return

        SetTrackStatusDialog(this, item).showDialog(router)
    }

    override fun onChaptersClick(position: Int) {
        val item = adapter?.getItem(position) ?: return
        if (item.track == null) return

        SetTrackChaptersDialog(this, item).showDialog(router)
    }

    override fun onScoreClick(position: Int) {
        val item = adapter?.getItem(position) ?: return
        if (item.track == null) return

        SetTrackScoreDialog(this, item).showDialog(router)
    }

    override fun setStatus(item: TrackItem, selection: Int) {
        presenter.setStatus(item, selection)
        binding.swipeRefresh.isRefreshing = true
    }

    override fun setScore(item: TrackItem, score: Int) {
        presenter.setScore(item, score)
        binding.swipeRefresh.isRefreshing = true
    }

    override fun setChaptersRead(item: TrackItem, chaptersRead: Int) {
        presenter.setLastChapterRead(item, chaptersRead)
        binding.swipeRefresh.isRefreshing = true
    }

    private companion object {
        const val TAG_SEARCH_CONTROLLER = "track_search_controller"
    }
}
