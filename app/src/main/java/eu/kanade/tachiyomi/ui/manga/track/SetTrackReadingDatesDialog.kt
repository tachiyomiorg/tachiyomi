package eu.kanade.tachiyomi.ui.manga.track

import android.app.Dialog
import android.os.Bundle
import android.widget.NumberPicker
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.bluelinelabs.conductor.Controller
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.util.system.toast
import java.text.DateFormatSymbols
import java.util.Calendar
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SetTrackReadingDatesDialog<T> : DialogController
        where T : Controller, T : SetTrackReadingDatesDialog.Listener {

    private val item: TrackItem

    private val dateToUpdate: ReadingDate

    constructor(target: T, dateToUpdate: ReadingDate, item: TrackItem) : super(Bundle().apply {
        putSerializable(SetTrackReadingDatesDialog.KEY_ITEM_TRACK, item.track)
    }) {
        targetController = target
        this.item = item
        this.dateToUpdate = dateToUpdate
    }

    @Suppress("unused")
    constructor(bundle: Bundle) : super(bundle) {
        val track = bundle.getSerializable(SetTrackReadingDatesDialog.KEY_ITEM_TRACK) as Track
        val service = Injekt.get<TrackManager>().getService(track.sync_id)!!
        item = TrackItem(track, service)
        dateToUpdate = ReadingDate.Start
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        val item = item

        val dialog = MaterialDialog(activity!!)
                .title(when (dateToUpdate) {
                    ReadingDate.Start -> R.string.track_started_reading_date
                    ReadingDate.Finish -> R.string.track_finished_reading_date
                })
                .customView(R.layout.track_date_dialog, dialogWrapContent = false)
                .positiveButton(android.R.string.ok) { dialog ->
                    onDialogConfirm(dialog)
                }
                .negativeButton(android.R.string.cancel) { dialog ->
                    dialog.dismiss()
                }
                .neutralButton(R.string.action_remove) { dialog ->
                    val listener = (targetController as? Listener)
                    listener?.setReadingDate(item, dateToUpdate, 0L)
                    dialog.dismiss()
                }
                .noAutoDismiss()

        onDialogCreated(dialog)

        return dialog
    }

    private fun onDialogCreated(dialog: MaterialDialog) {
        val view = dialog.getCustomView()

        val dayPicker: NumberPicker = view.findViewById(R.id.day_picker)
        val monthPicker: NumberPicker = view.findViewById(R.id.month_picker)
        val yearPicker: NumberPicker = view.findViewById(R.id.year_picker)

        val monthNames: Array<String> = DateFormatSymbols().months
        monthPicker.displayedValues = monthNames

        val calendar = Calendar.getInstance()
        item.track?.let {
            val date = when (dateToUpdate) {
                ReadingDate.Start -> it.started_reading_date
                ReadingDate.Finish -> it.finished_reading_date
            }
            if (date != 0L)
                calendar.timeInMillis = date
        }
        dayPicker.value = calendar[Calendar.DAY_OF_MONTH]
        monthPicker.value = calendar[Calendar.MONTH]
        yearPicker.maxValue = calendar[Calendar.YEAR]
        yearPicker.value = calendar[Calendar.YEAR]
    }

    private fun onDialogConfirm(dialog: MaterialDialog) {
        val view = dialog.getCustomView()

        val dayPicker: NumberPicker = view.findViewById(R.id.day_picker)
        val monthPicker: NumberPicker = view.findViewById(R.id.month_picker)
        val yearPicker: NumberPicker = view.findViewById(R.id.year_picker)

        try {
            val calendar = Calendar.getInstance().apply { isLenient = false }
            calendar.set(yearPicker.value, monthPicker.value, dayPicker.value)
            calendar.time = calendar.time // Throws if invalid

            val listener = (targetController as? Listener)
            listener?.setReadingDate(item, dateToUpdate, calendar.timeInMillis)
            dialog.dismiss()
        } catch (e: Exception) {
            activity?.toast(R.string.error_invalid_date_supplied)
        }
    }

    interface Listener {
        fun setReadingDate(item: TrackItem, type: ReadingDate, date: Long)
    }

    enum class ReadingDate {
        Start,
        Finish
    }

    companion object {
        private const val KEY_ITEM_TRACK = "SetTrackReadingDatesDialog.item.track"
    }
}
