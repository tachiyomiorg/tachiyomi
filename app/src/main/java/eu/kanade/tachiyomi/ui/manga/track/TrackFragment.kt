package eu.kanade.tachiyomi.ui.manga.track

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import com.afollestad.materialdialogs.MaterialDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.MangaSync
import eu.kanade.tachiyomi.ui.base.fragment.BaseRxFragment
import eu.kanade.tachiyomi.ui.manga.MangaActivity
import eu.kanade.tachiyomi.util.toast
import kotlinx.android.synthetic.main.fragment_track.*
import nucleus.factory.RequiresPresenter

@RequiresPresenter(TrackPresenter::class)
class TrackFragment : BaseRxFragment<TrackPresenter>() {

    companion object {
        fun newInstance(): TrackFragment {
            return TrackFragment()
        }
    }

    private lateinit var adapter: TrackAdapter

    private var dialog: TrackSearchDialog? = null

    private val searchFragmentTag: String
        get() = "search_fragment"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_track, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = TrackAdapter(this)
        recycler.layoutManager = LinearLayoutManager(context)
        recycler.adapter = adapter
        swipe_refresh.isEnabled = false
        swipe_refresh.setOnRefreshListener { presenter.refresh() }
    }

    private fun findSearchFragmentIfNeeded() {
        if (dialog == null) {
            dialog = childFragmentManager.findFragmentByTag(searchFragmentTag) as TrackSearchDialog
        }
    }

    fun onNextTrackings(trackings: List<TrackItem>) {
        adapter.items = trackings
        swipe_refresh.isEnabled = trackings.any { it.sync != null }
        (activity as MangaActivity).setTrackingIcon(trackings.any { it.sync != null })
    }

    fun onSearchResults(results: List<MangaSync>) {
        if (!isResumed) return

        findSearchFragmentIfNeeded()
        dialog?.onSearchResults(results)
    }

    fun onSearchResultsError(error: Throwable) {
        if (!isResumed) return

        findSearchFragmentIfNeeded()
        dialog?.onSearchResultsError()
    }

    fun onRefreshDone() {
        swipe_refresh.isRefreshing = false
    }

    fun onRefreshError(error: Throwable) {
        swipe_refresh.isRefreshing = false
        context.toast(error.message)
    }

    fun onTitleClick(item: TrackItem) {
        if (!isResumed) return

        if (dialog == null) {
            dialog = TrackSearchDialog.newInstance()
        }

        presenter.selectedService = item.service
        dialog?.show(childFragmentManager, searchFragmentTag)
    }

    fun onStatusClick(item: TrackItem) {
        if (!isResumed || item.sync == null) return

        val statusList = item.service.getStatusList().map { item.service.getStatus(it) }
        val selectedIndex = item.service.getStatusList().indexOf(item.sync.status)

        MaterialDialog.Builder(context)
                .title(R.string.status)
                .items(statusList)
                .itemsCallbackSingleChoice(selectedIndex, { dialog, view, i, charSequence ->
                    presenter.setStatus(item, i)
                    swipe_refresh.isRefreshing = true
                    true
                })
                .show()
    }

    fun onChaptersClick(item: TrackItem) {
        if (!isResumed || item.sync == null) return

        val dialog = MaterialDialog.Builder(context)
                .title(R.string.chapters)
                .customView(R.layout.dialog_track_chapters, false)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .onPositive { d, action ->
                    val view = d.customView
                    if (view != null) {
                        val np = view.findViewById(R.id.chapters_picker) as NumberPicker
                        np.clearFocus()
                        presenter.setLastChapterRead(item, np.value)
                        swipe_refresh.isRefreshing = true
                    }
                }
                .show()

        val view = dialog.customView
        if (view != null) {
            val np = view.findViewById(R.id.chapters_picker) as NumberPicker
            // Set initial value
            np.value = item.sync.last_chapter_read
            // Don't allow to go from 0 to 9999
            np.wrapSelectorWheel = false
        }
    }

    fun onScoreClick(item: TrackItem) {
        if (!isResumed || item.sync == null) return

        val dialog = MaterialDialog.Builder(activity)
                .title(R.string.score)
                .customView(R.layout.dialog_track_score, false)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .onPositive { d, action ->
                    val view = d.customView
                    if (view != null) {
                        val np = view.findViewById(R.id.score_picker) as NumberPicker
                        np.clearFocus()
                        presenter.setScore(item, np.value)
                        swipe_refresh.isRefreshing = true
                    }
                }
                .show()

        val view = dialog.customView
        if (view != null) {
            val np = view.findViewById(R.id.score_picker) as NumberPicker
            np.maxValue = item.service.maxScore()
            // Set initial value
            np.value = item.sync.score.toInt()
        }
    }

}