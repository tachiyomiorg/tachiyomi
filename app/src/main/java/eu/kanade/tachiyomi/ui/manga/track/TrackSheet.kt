package eu.kanade.tachiyomi.ui.manga.track

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.track.EnhancedTrackService
import eu.kanade.tachiyomi.databinding.TrackControllerBinding
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.base.controller.openInBrowser
import eu.kanade.tachiyomi.ui.manga.MangaController
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.toLocalCalendar
import eu.kanade.tachiyomi.util.lang.toUtcCalendar
import eu.kanade.tachiyomi.util.lang.withUIContext
import eu.kanade.tachiyomi.util.system.copyToClipboard
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.widget.sheet.BaseBottomSheetDialog
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class TrackSheet(
    val controller: MangaController,
    val manga: Manga,
    val fragmentManager: FragmentManager,
    private val sourceManager: SourceManager = Injekt.get()
) : BaseBottomSheetDialog(controller.activity!!),
    TrackAdapter.OnClickListener,
    SetTrackStatusDialog.Listener,
    SetTrackChaptersDialog.Listener,
    SetTrackScoreDialog.Listener {

    private lateinit var binding: TrackControllerBinding

    private lateinit var adapter: TrackAdapter

    override fun createView(inflater: LayoutInflater): View {
        binding = TrackControllerBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = TrackAdapter(this)
        binding.trackRecycler.layoutManager = LinearLayoutManager(context)
        binding.trackRecycler.adapter = adapter

        adapter.items = controller.presenter.trackList
    }

    override fun show() {
        super.show()
        controller.presenter.refreshTrackers()
        behavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    fun onNextTrackers(trackers: List<TrackItem>) {
        if (this::adapter.isInitialized) {
            adapter.items = trackers
            adapter.notifyDataSetChanged()
        }
    }

    override fun onOpenInBrowserClick(position: Int) {
        val track = adapter.getItem(position)?.track ?: return

        if (track.tracking_url.isNotBlank()) {
            controller.openInBrowser(track.tracking_url)
        }
    }

    override fun onSetClick(position: Int) {
        val item = adapter.getItem(position) ?: return

        if (item.service is EnhancedTrackService) {
            if (item.track != null) {
                controller.presenter.unregisterTracking(item.service)
                return
            }

            if (!item.service.accept(sourceManager.getOrStub(manga.source))) {
                controller.presenter.view?.applicationContext?.toast(R.string.source_unsupported)
                return
            }

            launchIO {
                try {
                    item.service.match(manga)?.let { track ->
                        controller.presenter.registerTracking(track, item.service)
                    }
                        ?: withUIContext { controller.presenter.view?.applicationContext?.toast(R.string.error_no_match) }
                } catch (e: Exception) {
                    withUIContext { controller.presenter.view?.applicationContext?.toast(R.string.error_no_match) }
                }
            }
        } else {
            TrackSearchDialog(controller, item.service, item.track?.tracking_url)
                .showDialog(controller.router, TAG_SEARCH_CONTROLLER)
        }
    }

    override fun onTitleLongClick(position: Int) {
        adapter.getItem(position)?.track?.title?.let {
            controller.activity?.copyToClipboard(it, it)
        }
    }

    override fun onStatusClick(position: Int) {
        val item = adapter.getItem(position) ?: return
        if (item.track == null) return

        SetTrackStatusDialog(controller, this, item).showDialog(controller.router)
    }

    override fun onChaptersClick(position: Int) {
        val item = adapter.getItem(position) ?: return
        if (item.track == null) return

        SetTrackChaptersDialog(controller, this, item).showDialog(controller.router)
    }

    override fun onScoreClick(position: Int) {
        val item = adapter.getItem(position) ?: return
        if (item.track == null || item.service.getScoreList().isEmpty()) return

        SetTrackScoreDialog(controller, this, item).showDialog(controller.router)
    }

    override fun onStartDateEditClick(position: Int) {
        val item = adapter.getItem(position) ?: return
        if (item.track == null) return

        val selection = item.track.started_reading_date.toUtcCalendar()?.timeInMillis
            ?: MaterialDatePicker.todayInUtcMilliseconds()

        // No time travellers allowed
        val constraints = CalendarConstraints.Builder().apply {
            val finishedMillis = item.track.finished_reading_date.toUtcCalendar()?.timeInMillis
            if (finishedMillis != null) {
                setValidator(DateValidatorPointBackward.before(finishedMillis))
            }
        }.build()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.track_started_reading_date)
            .setSelection(selection)
            .setCalendarConstraints(constraints)
            .build()
        picker.addOnPositiveButtonClickListener { utcMillis ->
            val result = utcMillis.toLocalCalendar()?.timeInMillis
            if (result != null) {
                controller.presenter.setTrackerStartDate(item, result)
            }
        }
        picker.show(fragmentManager, null)
    }

    override fun onFinishDateEditClick(position: Int) {
        val item = adapter.getItem(position) ?: return
        if (item.track == null) return

        val selection = item.track.finished_reading_date.toUtcCalendar()?.timeInMillis
            ?: MaterialDatePicker.todayInUtcMilliseconds()

        // No time travellers allowed
        val constraints = CalendarConstraints.Builder().apply {
            val startMillis = item.track.started_reading_date.toUtcCalendar()?.timeInMillis
            if (startMillis != null) {
                setValidator(DateValidatorPointForward.from(startMillis))
            }
        }.build()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.track_finished_reading_date)
            .setSelection(selection)
            .setCalendarConstraints(constraints)
            .build()
        picker.addOnPositiveButtonClickListener { utcMillis ->
            val result = utcMillis.toLocalCalendar()?.timeInMillis
            if (result != null) {
                controller.presenter.setTrackerFinishDate(item, result)
            }
        }
        picker.show(fragmentManager, null)
    }

    override fun onStartDateRemoveClick(position: Int) {
        val item = adapter.getItem(position) ?: return
        if (item.track == null) return
        controller.presenter.setTrackerStartDate(item, 0)
    }

    override fun onFinishDateRemoveClick(position: Int) {
        val item = adapter.getItem(position) ?: return
        if (item.track == null) return
        controller.presenter.setTrackerFinishDate(item, 0)
    }

    override fun onRemoveItemClick(position: Int) {
        val item = adapter.getItem(position) ?: return
        if (item.track == null) return
        controller.presenter.unregisterTracking(item.service)
    }

    override fun setStatus(item: TrackItem, selection: Int) {
        controller.presenter.setTrackerStatus(item, selection)
    }

    override fun setChaptersRead(item: TrackItem, chaptersRead: Int) {
        controller.presenter.setTrackerLastChapterRead(item, chaptersRead)
    }

    override fun setScore(item: TrackItem, score: Int) {
        controller.presenter.setTrackerScore(item, score)
    }

    fun getSearchDialog(): TrackSearchDialog? {
        return controller.router.getControllerWithTag(TAG_SEARCH_CONTROLLER) as? TrackSearchDialog
    }

    private companion object {
        const val TAG_SEARCH_CONTROLLER = "track_search_controller"
    }
}
