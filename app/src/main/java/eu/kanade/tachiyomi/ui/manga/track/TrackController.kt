package eu.kanade.tachiyomi.ui.manga.track

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding.support.v4.widget.refreshes
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.manga.MangaController
import eu.kanade.tachiyomi.util.toast
import kotlinx.android.synthetic.main.fragment_track.view.*

class TrackController : NucleusController<TrackPresenter>(),
        TrackAdapter.OnRowClickListener,
        SetTrackStatusDialog.Listener,
        SetTrackChaptersDialog.Listener,
        SetTrackScoreDialog.Listener {

    private var adapter: TrackAdapter? = null

    override fun createPresenter(): TrackPresenter {
        return TrackPresenter((parentController as MangaController).manga!!)
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.fragment_track, container, false)
    }

    override fun onViewCreated(view: View, savedViewState: Bundle?) {
        super.onViewCreated(view, savedViewState)

        adapter = TrackAdapter(this)
        with(view) {
            track_recycler.layoutManager = LinearLayoutManager(context)
            track_recycler.adapter = adapter
            swipe_refresh.isEnabled = false
            swipe_refresh.refreshes().subscribeUntilDestroy { presenter.refresh() }
        }
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        adapter = null
    }

    fun onNextTrackings(trackings: List<TrackItem>) {
        val atLeastOneLink = trackings.any { it.track != null }
        adapter?.items = trackings
        view?.swipe_refresh?.isEnabled = atLeastOneLink
        (parentController as? MangaController)?.setTrackingIcon(atLeastOneLink)
    }

    fun onSearchResults(results: List<Track>) {
        getSearchDialog()?.onSearchResults(results)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onSearchResultsError(error: Throwable) {
        getSearchDialog()?.onSearchResultsError()
    }

    private fun getSearchDialog(): TrackSearchDialog? {
        return router.getControllerWithTag(TAG_SEARCH_CONTROLLER) as? TrackSearchDialog
    }

    fun onRefreshDone() {
        view?.swipe_refresh?.isRefreshing = false
    }

    fun onRefreshError(error: Throwable) {
        view?.swipe_refresh?.isRefreshing = false
        activity?.toast(error.message)
    }

    override fun onTitleClick(position: Int) {
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
        view?.swipe_refresh?.isRefreshing = true
    }

    override fun setScore(item: TrackItem, score: Int) {
        presenter.setScore(item, score)
        view?.swipe_refresh?.isRefreshing = true
    }

    override fun setChaptersRead(item: TrackItem, chaptersRead: Int) {
        presenter.setLastChapterRead(item, chaptersRead)
        view?.swipe_refresh?.isRefreshing = true
    }

    private companion object {
        const val TAG_SEARCH_CONTROLLER = "track_search_controller"
    }

}