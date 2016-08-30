package eu.kanade.tachiyomi.ui.reader

import android.app.Dialog
import android.graphics.Color
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

    private var red = 0
    private var blue = 0
    private var green = 0
    private var colorAlpha = 0


    override fun onCreateDialog(savedState: Bundle?): Dialog {
        val dialog = MaterialDialog.Builder(activity)
                .customView(R.layout.dialog_reader_custom_filter, false)
                .positiveText(android.R.string.ok)
                .build()

        onViewCreated(dialog.view, savedState)

        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = with(view) {
        red =  preferences.colorFilterRedValue().getOrDefault()
        colorAlpha =  preferences.colorFilterAlphaValue().getOrDefault()

        //Initialize values
        txt_color_filter_red_value.text = red.toString()
        seekbar_color_filter_red.progress = red

        txt_color_filter_alpha_value.text =  colorAlpha.toString()
        seekbar_color_filter_alpha.progress = colorAlpha

        switch_color_filter.isChecked = preferences.colorFilter().getOrDefault()
        switch_color_filter.setOnCheckedChangeListener { v, isChecked ->
            preferences.colorFilter().set(isChecked)
        }

        seekbar_color_filter_red.setOnProgressChangeListener(object : DiscreteSeekBar.OnProgressChangeListener {
            override fun onProgressChanged(seekBar: DiscreteSeekBar, value: Int, fromUser: Boolean) {
                if (fromUser) {
                    preferences.colorFilterRedValue().set(value)
                    setColorFilterRedValue(value, view)
                }
            }

            override fun onStartTrackingTouch(seekBar: DiscreteSeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: DiscreteSeekBar) {
            }
        })

        seekbar_color_filter_alpha.setOnProgressChangeListener(object : DiscreteSeekBar.OnProgressChangeListener {
            override fun onProgressChanged(seekBar: DiscreteSeekBar, value: Int, fromUser: Boolean) {
                if (fromUser) {
                    preferences.colorFilterAlphaValue().set(value)
                    setColorFilterAlphaValue(value, view)
                }
            }

            override fun onStartTrackingTouch(seekBar: DiscreteSeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: DiscreteSeekBar) {
            }
        })

        setColorFilter(view)

    }

    private fun setColorFilterRedValue(value: Int, view: View)  = with(view)  {
        red = value
        txt_color_filter_red_value.text =  value.toString()
        setColorFilter(view)
    }

    private fun setColorFilterAlphaValue(value: Int, view: View)  = with(view)  {
        colorAlpha = value
        txt_color_filter_alpha_value.text =  value.toString()
        setColorFilter(view)
    }
    private fun setColorFilter(view: View) = with(view) {
        if (alpha >= 0) {
            color_view.visibility = View.VISIBLE
            val alphaVal = Math.abs(colorAlpha * 2.56).toInt()
            color_view.setBackgroundColor(Color.argb(alphaVal, red, blue, green))
        }
    }

}