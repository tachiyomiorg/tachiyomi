package eu.kanade.tachiyomi.ui.manga.track

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.databinding.TrackControllerBinding
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.util.system.copyToClipboard
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import reactivecircus.flowbinding.swiperefreshlayout.refreshes
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class TrackController :
    NucleusController<TrackControllerBinding, TrackPresenter>,
    TrackAdapter.OnClickListener,
    SetTrackStatusDialog.Listener,
    SetTrackChaptersDialog.Listener,
    SetTrackScoreDialog.Listener,
    SetTrackReadingDatesDialog.Listener,
    GetTrackChaptersDialog.Listener {

    constructor(manga: Manga?) : super(
        Bundle().apply {
            putLong(MANGA_EXTRA, manga?.id ?: 0)
        }
    ) {
        this.manga = manga
    }

    constructor(mangaId: Long) : this(
        Injekt.get<DatabaseHelper>().getManga(mangaId).executeAsBlocking()
    )

    @Suppress("unused")
    constructor(bundle: Bundle) : this(bundle.getLong(MANGA_EXTRA))

    var manga: Manga? = null
        private set

    private var adapter: TrackAdapter? = null

    init {
        // There's no menu, but this avoids a bug when coming from the catalogue, where the menu
        // disappears if the searchview is expanded
        setHasOptionsMenu(true)
    }

    override fun getTitle(): String? {
        return manga?.title
    }

    override fun createPresenter(): TrackPresenter {
        return TrackPresenter(manga!!)
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        binding = TrackControllerBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        if (manga == null) return

        adapter = TrackAdapter(this)
        binding.trackRecycler.layoutManager = LinearLayoutManager(view.context)
        binding.trackRecycler.adapter = adapter
        binding.swipeRefresh.isEnabled = false
        binding.swipeRefresh.refreshes()
            .onEach { presenter.refresh() }
            .launchIn(scope)
    }

    override fun onDestroyView(view: View) {
        adapter = null
        super.onDestroyView(view)
    }

    fun onNextTrackings(trackings: List<TrackItem>) {
        val atLeastOneLink = trackings.any { it.track != null }
        adapter?.items = trackings
        binding.swipeRefresh.isEnabled = atLeastOneLink
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

        if (track.tracking_url.isNotBlank()) {
            activity?.startActivity(Intent(Intent.ACTION_VIEW, track.tracking_url.toUri()))
        }
    }

    override fun onSetClick(position: Int) {
        val item = adapter?.getItem(position) ?: return
        TrackSearchDialog(this, item.service).showDialog(router, TAG_SEARCH_CONTROLLER)
    }

    override fun onTitleLongClick(position: Int) {
        adapter?.getItem(position)?.track?.title?.let {
            activity?.copyToClipboard(it, it)
        }
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

        SetTrackReadingDatesDialog(this, SetTrackReadingDatesDialog.ReadingDate.Start, item).showDialog(router)
    }

    override fun onFinishDateClick(position: Int) {
        val item = adapter?.getItem(position) ?: return
        if (item.track == null) return

        SetTrackReadingDatesDialog(this, SetTrackReadingDatesDialog.ReadingDate.Finish, item).showDialog(router)
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

    override fun setReadingDate(item: TrackItem, type: SetTrackReadingDatesDialog.ReadingDate, date: Long) {
        when (type) {
            SetTrackReadingDatesDialog.ReadingDate.Start -> presenter.setStartDate(item, date)
            SetTrackReadingDatesDialog.ReadingDate.Finish -> presenter.setFinishDate(item, date)
        }
        binding.swipeRefresh.isRefreshing = true
    }

    override fun onGetChaptersClick(position: Int) {
        val item = adapter?.getItem(position) ?: return
        if (item.track == null) return

        // get list of chapters from source to send to dialog
        val chapters = presenter.getSourceChapters()

        GetTrackChaptersDialog(this, item, chapters).showDialog(router)
    }

    override fun getChaptersRead(latestTrackedChapter: Int) {
        presenter.syncChaptersRead(latestTrackedChapter)
    }

    private companion object {
        const val MANGA_EXTRA = "manga"
        const val TAG_SEARCH_CONTROLLER = "track_search_controller"
    }
}
