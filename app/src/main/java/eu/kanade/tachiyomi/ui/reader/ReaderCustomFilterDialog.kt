package eu.kanade.tachiyomi.ui.reader

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import kotlinx.android.synthetic.main.dialog_reader_custom_filter.view.*
import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar
import uy.kohesive.injekt.injectLazy

class ReaderCustomFilterDialog : DialogFragment() {

    private val preferences by injectLazy<PreferencesHelper>()

    override fun onCreateDialog(savedState: Bundle?): Dialog {
        val dialog = MaterialDialog.Builder(activity)
                .customView(R.layout.dialog_reader_custom_filter, false)
                .positiveText(android.R.string.ok)
                .build()

        onViewCreated(dialog.view, savedState)

        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = with(view) {
        red_filter_seekbar.progress = preferences.redFilterValue().getOrDefault()
        red_filter_seekbar.setOnProgressChangeListener(object : DiscreteSeekBar.OnProgressChangeListener {
            override fun onProgressChanged(seekBar: DiscreteSeekBar, value: Int, fromUser: Boolean) {
                if (fromUser) {
                    preferences.redFilterValue().set(value)
                }
            }

            override fun onStartTrackingTouch(seekBar: DiscreteSeekBar) {}

            override fun onStopTrackingTouch(seekBar: DiscreteSeekBar) {}
        })
    }

}