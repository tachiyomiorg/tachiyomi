package eu.kanade.tachiyomi.ui.manga.track

import android.app.Dialog
import android.os.Bundle
import android.widget.NumberPicker
import com.afollestad.materialdialogs.MaterialDialog
import com.bluelinelabs.conductor.Controller
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.util.system.toast
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.GregorianCalendar
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SetTrackReadingDatesDialog<T> : DialogController
        where T : Controller, T : SetTrackReadingDatesDialog.Listener {

    private val item: TrackItem

    private val setting: Int

    constructor(target: T, setting: Int, item: TrackItem) : super(Bundle().apply {
        putSerializable(SetTrackReadingDatesDialog.KEY_ITEM_TRACK, item.track)
    }) {
        targetController = target
        this.item = item
        this.setting = setting
    }

    @Suppress("unused")
    constructor(bundle: Bundle) : super(bundle) {
        val track = bundle.getSerializable(SetTrackReadingDatesDialog.KEY_ITEM_TRACK) as Track
        val service = Injekt.get<TrackManager>().getService(track.sync_id)!!
        item = TrackItem(track, service)
        this.setting = SET_START_DATE
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        val item = item
        val isStart = setting == SET_START_DATE

        val dialog = MaterialDialog.Builder(activity!!)
                .title(if (isStart) R.string.track_start_date else R.string.track_finish_date)
                .customView(R.layout.track_date_dialog, false)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .neutralText(R.string.action_remove)
                .autoDismiss(false)
                .onNegative { dialog, _ -> dismissDialog() }
                .onNeutral { dialog, _ ->
                    val listener = (targetController as Listener)
                    if (isStart)
                        listener.setStartDate(item, null)
                    else
                        listener.setFinishDate(item, null)
                    dismissDialog()
                }
                .onPositive { dialog, _ ->
                    val view = dialog.customView
                    if (view != null) {
                        val dayPicker: NumberPicker = view.findViewById(R.id.day_picker)
                        val monthPicker: NumberPicker = view.findViewById(R.id.month_picker)
                        val yearPicker: NumberPicker = view.findViewById(R.id.year_picker)

                        try {
                            val calendar = GregorianCalendar.getInstance()
                            calendar.isLenient = false
                            calendar.set(yearPicker.value, monthPicker.value, dayPicker.value)
                            calendar.time = calendar.time // Throws if invalid

                            val listener = (targetController as Listener)
                            if (isStart)
                                listener.setStartDate(item, calendar)
                            else
                                listener.setFinishDate(item, calendar)
                            dismissDialog()
                        } catch (e: Exception) {
                            activity?.toast(R.string.error_invalid_date_supplied)
                        }
                    }
                }
                .show()

        val view = dialog.customView
        if (view != null) {
            val today = GregorianCalendar.getInstance()

            val dayPicker: NumberPicker = view.findViewById(R.id.day_picker)
            val monthPicker: NumberPicker = view.findViewById(R.id.month_picker)
            val yearPicker: NumberPicker = view.findViewById(R.id.year_picker)

            val monthNames: Array<String> = DateFormatSymbols().months
            monthPicker.displayedValues = monthNames

            dayPicker.value = today.get(GregorianCalendar.DAY_OF_MONTH)
            monthPicker.value = today.get(GregorianCalendar.MONTH)
            yearPicker.maxValue = today.get(GregorianCalendar.YEAR)
            yearPicker.value = today.get(GregorianCalendar.YEAR)
        }

        return dialog
    }

    interface Listener {
        fun setStartDate(item: TrackItem, date: Calendar?)
        fun setFinishDate(item: TrackItem, date: Calendar?)
    }

    companion object {
        private const val KEY_ITEM_TRACK = "SetTrackReadingDatesDialog.item.track"

        const val SET_START_DATE = 1
        const val SET_FINISH_DATE = 2
    }
}
