package eu.kanade.tachiyomi.ui.reader

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.view.gone
import eu.kanade.tachiyomi.util.view.visible
import eu.kanade.tachiyomi.widget.IgnoreFirstSpinnerListener
import kotlin.math.abs
import kotlinx.android.synthetic.main.reader_color_filter.brightness_seekbar
import kotlinx.android.synthetic.main.reader_color_filter.color_filter_mode
import kotlinx.android.synthetic.main.reader_color_filter.custom_brightness
import kotlinx.android.synthetic.main.reader_color_filter.seekbar_color_filter_alpha
import kotlinx.android.synthetic.main.reader_color_filter.seekbar_color_filter_blue
import kotlinx.android.synthetic.main.reader_color_filter.seekbar_color_filter_green
import kotlinx.android.synthetic.main.reader_color_filter.seekbar_color_filter_red
import kotlinx.android.synthetic.main.reader_color_filter.switch_color_filter
import kotlinx.android.synthetic.main.reader_color_filter.txt_brightness_seekbar_value
import kotlinx.android.synthetic.main.reader_color_filter.txt_color_filter_alpha_value
import kotlinx.android.synthetic.main.reader_color_filter.txt_color_filter_blue_value
import kotlinx.android.synthetic.main.reader_color_filter.txt_color_filter_green_value
import kotlinx.android.synthetic.main.reader_color_filter.txt_color_filter_red_value
import kotlinx.android.synthetic.main.reader_color_filter_sheet.brightness_overlay
import kotlinx.android.synthetic.main.reader_color_filter_sheet.color_overlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import reactivecircus.flowbinding.android.widget.progressChanges
import uy.kohesive.injekt.injectLazy

/**
 * Color filter sheet to toggle custom filter and brightness overlay.
 */
class ReaderColorFilterSheet(private val activity: ReaderActivity) : BottomSheetDialog(activity) {

    private val scope = CoroutineScope(Job() + Dispatchers.Main)

    private val preferences by injectLazy<PreferencesHelper>()

    private var sheetBehavior: BottomSheetBehavior<*>? = null

    init {
        val view = activity.layoutInflater.inflate(R.layout.reader_color_filter_sheet, null)
        setContentView(view)

        sheetBehavior = BottomSheetBehavior.from(view.parent as ViewGroup)

        preferences.colorFilter().asFlow()
            .onEach { setColorFilter(it, view) }
            .launchIn(activity.scope)

        preferences.colorFilterMode().asFlow()
            .onEach { setColorFilter(preferences.colorFilter().get(), view) }
            .launchIn(activity.scope)

        preferences.customBrightness().asFlow()
            .onEach { setCustomBrightness(it, view) }
            .launchIn(activity.scope)

        // Get color and update values
        val color = preferences.colorFilterValue().get()
        val brightness = preferences.customBrightnessValue().get()

        val argb = setValues(color, view)

        // Set brightness value
        txt_brightness_seekbar_value.text = brightness.toString()
        brightness_seekbar.progress = brightness

        // Initialize seekBar progress
        seekbar_color_filter_alpha.progress = argb[0]
        seekbar_color_filter_red.progress = argb[1]
        seekbar_color_filter_green.progress = argb[2]
        seekbar_color_filter_blue.progress = argb[3]

        // Set listeners
        switch_color_filter.isChecked = preferences.colorFilter().get()
        switch_color_filter.setOnCheckedChangeListener { _, isChecked ->
            preferences.colorFilter().set(isChecked)
        }

        custom_brightness.isChecked = preferences.customBrightness().get()
        custom_brightness.setOnCheckedChangeListener { _, isChecked ->
            preferences.customBrightness().set(isChecked)
        }

        color_filter_mode.onItemSelectedListener = IgnoreFirstSpinnerListener { position ->
            preferences.colorFilterMode().set(position)
        }
        color_filter_mode.setSelection(preferences.colorFilterMode().get(), false)

        seekbar_color_filter_alpha.progressChanges()
            .onEach { setColorValue(it, ALPHA_MASK, 24) }
            .launchIn(scope)

        seekbar_color_filter_red.progressChanges()
            .onEach { setColorValue(it, RED_MASK, 16) }
            .launchIn(scope)

        seekbar_color_filter_green.progressChanges()
            .onEach { setColorValue(it, GREEN_MASK, 8) }
            .launchIn(scope)

        seekbar_color_filter_blue.progressChanges()
            .onEach { setColorValue(it, BLUE_MASK, 0) }
            .launchIn(scope)

        brightness_seekbar.progressChanges()
            .onEach { preferences.customBrightnessValue().set(it) }
            .launchIn(scope)
    }

    override fun onStart() {
        super.onStart()
        sheetBehavior?.skipCollapsed = true
        sheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    /**
     * Set enabled status of seekBars belonging to color filter
     * @param enabled determines if seekBar gets enabled
     * @param view view of the dialog
     */
    private fun setColorFilterSeekBar(enabled: Boolean, view: View) = with(view) {
        seekbar_color_filter_red.isEnabled = enabled
        seekbar_color_filter_green.isEnabled = enabled
        seekbar_color_filter_blue.isEnabled = enabled
        seekbar_color_filter_alpha.isEnabled = enabled
    }

    /**
     * Set enabled status of seekBars belonging to custom brightness
     * @param enabled value which determines if seekBar gets enabled
     * @param view view of the dialog
     */
    private fun setCustomBrightnessSeekBar(enabled: Boolean, view: View) = with(view) {
        brightness_seekbar.isEnabled = enabled
    }

    /**
     * Set the text value's of color filter
     * @param color integer containing color information
     * @param view view of the dialog
     */
    fun setValues(color: Int, view: View): Array<Int> {
        val alpha = getAlphaFromColor(color)
        val red = getRedFromColor(color)
        val green = getGreenFromColor(color)
        val blue = getBlueFromColor(color)

        // Initialize values
        txt_color_filter_alpha_value.text = alpha.toString()
        txt_color_filter_red_value.text = red.toString()
        txt_color_filter_green_value.text = green.toString()
        txt_color_filter_blue_value.text = blue.toString()

        return arrayOf(alpha, red, green, blue)
    }

    /**
     * Manages the custom brightness value subscription
     * @param enabled determines if the subscription get (un)subscribed
     * @param view view of the dialog
     */
    private fun setCustomBrightness(enabled: Boolean, view: View) {
        if (enabled) {
            preferences.customBrightnessValue().asFlow()
                .sample(100)
                .onEach { setCustomBrightnessValue(it, view) }
                .launchIn(activity.scope)
        } else {
            setCustomBrightnessValue(0, view, true)
        }
        setCustomBrightnessSeekBar(enabled, view)
    }

    /**
     * Sets the brightness of the screen. Range is [-75, 100].
     * From -75 to -1 a semi-transparent black view is shown at the top with the minimum brightness.
     * From 1 to 100 it sets that value as brightness.
     * 0 sets system brightness and hides the overlay.
     */
    private fun setCustomBrightnessValue(value: Int, view: View, isDisabled: Boolean = false) = with(view) {
        // Set black overlay visibility.
        if (value < 0) {
            brightness_overlay.visible()
            val alpha = (abs(value) * 2.56).toInt()
            brightness_overlay.setBackgroundColor(Color.argb(alpha, 0, 0, 0))
        } else {
            brightness_overlay.gone()
        }

        if (!isDisabled)
            txt_brightness_seekbar_value.text = value.toString()
    }

    /**
     * Manages the color filter value subscription
     * @param enabled determines if the subscription get (un)subscribed
     * @param view view of the dialog
     */
    private fun setColorFilter(enabled: Boolean, view: View) {
        if (enabled) {
            preferences.colorFilterValue().asFlow()
                .sample(100)
                .onEach { setColorFilterValue(it, view) }
                .launchIn(activity.scope)
        } else {
            color_overlay.gone()
        }
        setColorFilterSeekBar(enabled, view)
    }

    /**
     * Sets the color filter overlay of the screen. Determined by HEX of integer
     * @param color hex of color.
     * @param view view of the dialog
     */
    private fun setColorFilterValue(@ColorInt color: Int, view: View) = with(view) {
        color_overlay.visible()
        color_overlay.setFilterColor(color, preferences.colorFilterMode().get())
        setValues(color, view)
    }

    /**
     * Updates the color value in preference
     * @param color value of color range [0,255]
     * @param mask contains hex mask of chosen color
     * @param bitShift amounts of bits that gets shifted to receive value
     */
    fun setColorValue(color: Int, mask: Long, bitShift: Int) {
        val currentColor = preferences.colorFilterValue().get()
        val updatedColor = (color shl bitShift) or (currentColor and mask.inv().toInt())
        preferences.colorFilterValue().set(updatedColor)
    }

    /**
     * Returns the alpha value from the Color Hex
     * @param color color hex as int
     * @return alpha of color
     */
    fun getAlphaFromColor(color: Int): Int {
        return color shr 24 and 0xFF
    }

    /**
     * Returns the red value from the Color Hex
     * @param color color hex as int
     * @return red of color
     */
    fun getRedFromColor(color: Int): Int {
        return color shr 16 and 0xFF
    }

    /**
     * Returns the green value from the Color Hex
     * @param color color hex as int
     * @return green of color
     */
    fun getGreenFromColor(color: Int): Int {
        return color shr 8 and 0xFF
    }

    /**
     * Returns the blue value from the Color Hex
     * @param color color hex as int
     * @return blue of color
     */
    fun getBlueFromColor(color: Int): Int {
        return color and 0xFF
    }

    private companion object {
        /** Integer mask of alpha value **/
        const val ALPHA_MASK: Long = 0xFF000000

        /** Integer mask of red value **/
        const val RED_MASK: Long = 0x00FF0000

        /** Integer mask of green value **/
        const val GREEN_MASK: Long = 0x0000FF00

        /** Integer mask of blue value **/
        const val BLUE_MASK: Long = 0x000000FF
    }
}
