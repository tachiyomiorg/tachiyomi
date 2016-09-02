package eu.kanade.tachiyomi.ui.reader

import android.app.Dialog
import android.os.Bundle
import android.support.annotation.ColorInt
import android.support.v4.app.DialogFragment
import android.view.View
import android.widget.SeekBar
import com.afollestad.materialdialogs.MaterialDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.util.plusAssign
import eu.kanade.tachiyomi.widget.SimpleSeekBarListener
import kotlinx.android.synthetic.main.dialog_reader_custom_filter.view.*
import rx.Subscription
import rx.subscriptions.CompositeSubscription
import uy.kohesive.injekt.injectLazy

class ReaderCustomFilterDialog : DialogFragment() {

    companion object {
        private const val ALPHA_MASK: Long = 0xFF000000
        private const val RED_MASK: Long = 0x00FF0000
        private const val GREEN_MASK: Long = 0x0000FF00
        private const val BLUE_MASK: Long = 0x000000FF
    }

    private val preferences by injectLazy<PreferencesHelper>()

    private lateinit var subscriptions: CompositeSubscription

    private var customFilterColorSubscription: Subscription? = null

    override fun onCreateDialog(savedState: Bundle?): Dialog {
        val dialog = MaterialDialog.Builder(activity)
                .customView(R.layout.dialog_reader_custom_filter, false)
                .positiveText(android.R.string.ok)
                .build()

        subscriptions = CompositeSubscription()
        onViewCreated(dialog.view, savedState)

        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = with(view) {
        subscriptions += preferences.colorFilter().asObservable()
                .subscribe { setColorFilter(it, view) }

        val color = preferences.colorFilterBlueValue().getOrDefault()
        val argb = setValues(color, view)

        //Initialize seekbar progress
        seekbar_color_filter_alpha.progress = argb[0]
        seekbar_color_filter_red.progress = argb[1]
        seekbar_color_filter_green.progress = argb[2]
        seekbar_color_filter_blue.progress = argb[3]

        switch_color_filter.isChecked = preferences.colorFilter().getOrDefault()
        switch_color_filter.setOnCheckedChangeListener { v, isChecked ->
            preferences.colorFilter().set(isChecked)
        }

        seekbar_color_filter_alpha.setOnSeekBarChangeListener(object : SimpleSeekBarListener() {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    setColorValue(progress, ALPHA_MASK, 24)
                }
            }
        })

        seekbar_color_filter_red.setOnSeekBarChangeListener(object : SimpleSeekBarListener() {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    setColorValue(progress, RED_MASK, 16)
                }
            }
        })

        seekbar_color_filter_green.setOnSeekBarChangeListener(object : SimpleSeekBarListener() {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    setColorValue(progress, GREEN_MASK, 8)
                }
            }
        })

        seekbar_color_filter_blue.setOnSeekBarChangeListener(object : SimpleSeekBarListener() {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    setColorValue(progress, BLUE_MASK, 0)
                }
            }
        })

    }

    private fun setSeekBar(enabled: Boolean, view: View) = with(view) {
        seekbar_color_filter_red.isEnabled = enabled
        seekbar_color_filter_green.isEnabled = enabled
        seekbar_color_filter_blue.isEnabled = enabled
        seekbar_color_filter_alpha.isEnabled = enabled
    }

    fun setValues(color: Int, view: View): Array<Int> {
        val alpha = getAlphaFromColor(color)
        val red = getRedFromColor(color)
        val green = getGreenFromColor(color)
        val blue = getBlueFromColor(color)

        //Initialize values
        with(view) {
            txt_color_filter_alpha_value.text = alpha.toString()

            txt_color_filter_red_value.text = red.toString()

            txt_color_filter_green_value.text = green.toString()

            txt_color_filter_blue_value.text = blue.toString()
        }
        return arrayOf(alpha, red, green, blue)
    }

    private fun setColorFilter(enabled: Boolean, view: View) {
        if (enabled) {
            customFilterColorSubscription = preferences.colorFilterBlueValue().asObservable()
                    .subscribe { setColorFilterValue(it, view) }

            subscriptions.add(customFilterColorSubscription)
        } else {
            customFilterColorSubscription?.let { subscriptions.remove(it) }
            view.color_view.visibility = View.GONE
        }
        setSeekBar(enabled, view)
    }

    private fun setColorFilterValue(@ColorInt color: Int, view: View) = with(view) {
            color_view.visibility = View.VISIBLE
        color_view.setBackgroundColor(color)
        setValues(color, view)
    }

    fun setColorValue(color: Int, mask: Long, bitShift: Int) {
        val currentColor = preferences.colorFilterBlueValue().getOrDefault()
        val updatedColor = (color shl bitShift) or (currentColor and mask.inv().toInt())
        preferences.colorFilterBlueValue().set(updatedColor)
    }

    /**
     * Returns the alpha value from the Color Hex
     *
     * @param color color hex as int
     * @return alpha of color
     */
    fun getAlphaFromColor(color: Int): Int {
        return color shr 24 and 0xFF
    }

    /**
     * Returns the red value from the Color Hex
     *
     * @param color color hex as int
     * @return red of color
     */
    fun getRedFromColor(color: Int): Int {
        return color shr 16 and 0xFF
    }

    /**
     * Returns the green value from the Color Hex
     *
     * @param color color hex as int
     * @return green of color
     */
    fun getGreenFromColor(color: Int): Int {
        return color shr 8 and 0xFF
    }

    /**
     * Returns the blue value from the Color Hex
     *
     * @param color color hex as int
     * @return blue of color
     */
    fun getBlueFromColor(color: Int): Int {
        return color and 0xFF
    }

    override fun onDestroyView() {
        subscriptions.unsubscribe()
        super.onDestroyView()
    }

}