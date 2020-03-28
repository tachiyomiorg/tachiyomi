package eu.kanade.tachiyomi.ui.manga.track

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.jakewharton.rxbinding.support.v4.widget.refreshes
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.manga.MangaController
import eu.kanade.tachiyomi.util.system.toast
import java.util.Calendar
import kotlinx.android.synthetic.main.track_controller.swipe_refresh
import kotlinx.android.synthetic.main.track_controller.track_recycler
import timber.log.Timber

class TrackController : NucleusController<TrackPresenter>(),
        TrackAdapter.OnClickListener,
        SetTrackStatusDialog.Listener,
        SetTrackChaptersDialog.Listener,
        SetTrackScoreDialog.Listener,
        SetTrackReadingDatesDialog.Listener {

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
        return inflater.inflate(R.layout.track_controller, container, false)
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        adapter = TrackAdapter(this)
        with(view) {
            track_recycler.layoutManager = LinearLayoutManager(context)
            track_recycler.adapter = adapter
            swipe_refresh.isEnabled = false
            swipe_refresh.refreshes().subscribeUntilDestroy { presenter.refresh() }
        }
    }

    override fun onDestroyView(view: View) {
        adapter = null
        super.onDestroyView(view)
    }

    fun onNextTrackings(trackings: List<TrackItem>) {
        val atLeastOneLink = trackings.any { it.track != null }
        adapter?.items = trackings
        swipe_refresh?.isEnabled = atLeastOneLink
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
        swipe_refresh?.isRefreshing = false
    }

    fun onRefreshError(error: Throwable) {
        swipe_refresh?.isRefreshing = false
        activity?.toast(error.message)
    }

    override fun onLogoClick(position: Int) {
        val track = adapter?.getItem(position)?.track ?: return

        if (track.tracking_url.isNullOrBlank()) {
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

    override fun onStartDateClick(position: Int) {
        val item = adapter?.getItem(position) ?: return
        if (item.track == null) return

        SetTrackReadingDatesDialog(this, SetTrackReadingDatesDialog.SET_START_DATE, item).showDialog(router)
    }

    override fun onFinishDateClick(position: Int) {
        val item = adapter?.getItem(position) ?: return
        if (item.track == null) return

        SetTrackReadingDatesDialog(this, SetTrackReadingDatesDialog.SET_FINISH_DATE, item).showDialog(router)
    }

    override fun setStatus(item: TrackItem, selection: Int) {
        presenter.setStatus(item, selection)
        swipe_refresh?.isRefreshing = true
    }

    override fun setScore(item: TrackItem, score: Int) {
        presenter.setScore(item, score)
        swipe_refresh?.isRefreshing = true
    }

    override fun setChaptersRead(item: TrackItem, chaptersRead: Int) {
        presenter.setLastChapterRead(item, chaptersRead)
        swipe_refresh?.isRefreshing = true
    }

    override fun setStartDate(item: TrackItem, date: Calendar?) {
        presenter.setStartDate(item, date)
        swipe_refresh?.isRefreshing = true
    }

    override fun setFinishDate(item: TrackItem, date: Calendar?) {
        presenter.setFinishDate(item, date)
        swipe_refresh?.isRefreshing = true
    }

    private companion object {
        const val TAG_SEARCH_CONTROLLER = "track_search_controller"
    }
}
