package eu.kanade.tachiyomi.ui.reader.setting

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.SeekBar
import androidx.annotation.ColorInt
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.ReaderColorFilterSettingsBinding
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.preference.bindToPreference
import eu.kanade.tachiyomi.widget.listener.SimpleSeekBarListener
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import uy.kohesive.injekt.injectLazy

/**
 * Color filter sheet to toggle custom filter and brightness overlay.
 */
class ReaderColorFilterSettings @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    NestedScrollView(context, attrs) {

    private val preferences: PreferencesHelper by injectLazy()

    private val binding = ReaderColorFilterSettingsBinding.inflate(LayoutInflater.from(context), this, false)

    init {
        addView(binding.root)

        preferences.colorFilter().asFlow()
            .onEach { setColorFilter(it) }
            .launchIn((context as ReaderActivity).lifecycleScope)

        preferences.colorFilterMode().asFlow()
            .onEach { setColorFilter(preferences.colorFilter().get()) }
            .launchIn(context.lifecycleScope)

        preferences.customBrightness().asFlow()
            .onEach { setCustomBrightness(it) }
            .launchIn(context.lifecycleScope)

        // Get color and update values
        val color = preferences.colorFilterValue().get()
        val brightness = preferences.customBrightnessValue().get()

        val argb = setValues(color)

        // Set brightness value
        binding.txtBrightnessSeekbarValue.text = brightness.toString()
        binding.brightnessSeekbar.progress = brightness

        // Initialize seekBar progress
        binding.seekbarColorFilterAlpha.progress = argb[0]
        binding.seekbarColorFilterRed.progress = argb[1]
        binding.seekbarColorFilterGreen.progress = argb[2]
        binding.seekbarColorFilterBlue.progress = argb[3]

        // Set listeners
        binding.switchColorFilter.bindToPreference(preferences.colorFilter())
        binding.customBrightness.bindToPreference(preferences.customBrightness())
        binding.colorFilterMode.bindToPreference(preferences.colorFilterMode())
        binding.grayscale.bindToPreference(preferences.grayscale())

        binding.seekbarColorFilterAlpha.setOnSeekBarChangeListener(
            object : SimpleSeekBarListener() {
                override fun onProgressChanged(seekBar: SeekBar, value: Int, fromUser: Boolean) {
                    if (fromUser) {
                        setColorValue(value, ALPHA_MASK, 24)
                    }
                }
            }
        )

        binding.seekbarColorFilterRed.setOnSeekBarChangeListener(
            object : SimpleSeekBarListener() {
                override fun onProgressChanged(seekBar: SeekBar, value: Int, fromUser: Boolean) {
                    if (fromUser) {
                        setColorValue(value, RED_MASK, 16)
                    }
                }
            }
        )

        binding.seekbarColorFilterGreen.setOnSeekBarChangeListener(
            object : SimpleSeekBarListener() {
                override fun onProgressChanged(seekBar: SeekBar, value: Int, fromUser: Boolean) {
                    if (fromUser) {
                        setColorValue(value, GREEN_MASK, 8)
                    }
                }
            }
        )

        binding.seekbarColorFilterBlue.setOnSeekBarChangeListener(
            object : SimpleSeekBarListener() {
                override fun onProgressChanged(seekBar: SeekBar, value: Int, fromUser: Boolean) {
                    if (fromUser) {
                        setColorValue(value, BLUE_MASK, 0)
                    }
                }
            }
        )

        binding.brightnessSeekbar.setOnSeekBarChangeListener(
            object : SimpleSeekBarListener() {
                override fun onProgressChanged(seekBar: SeekBar, value: Int, fromUser: Boolean) {
                    if (fromUser) {
                        preferences.customBrightnessValue().set(value)
                    }
                }
            }
        )
    }

    /**
     * Set enabled status of seekBars belonging to color filter
     * @param enabled determines if seekBar gets enabled
     */
    private fun setColorFilterSeekBar(enabled: Boolean) {
        binding.seekbarColorFilterRed.isEnabled = enabled
        binding.seekbarColorFilterGreen.isEnabled = enabled
        binding.seekbarColorFilterBlue.isEnabled = enabled
        binding.seekbarColorFilterAlpha.isEnabled = enabled
    }

    /**
     * Set enabled status of seekBars belonging to custom brightness
     * @param enabled value which determines if seekBar gets enabled
     */
    private fun setCustomBrightnessSeekBar(enabled: Boolean) {
        binding.brightnessSeekbar.isEnabled = enabled
    }

    /**
     * Set the text value's of color filter
     * @param color integer containing color information
     */
    fun setValues(color: Int): Array<Int> {
        val alpha = color.alpha
        val red = color.red
        val green = color.green
        val blue = color.blue

        // Initialize values
        binding.txtColorFilterAlphaValue.text = "$alpha"
        binding.txtColorFilterRedValue.text = "$red"
        binding.txtColorFilterGreenValue.text = "$green"
        binding.txtColorFilterBlueValue.text = "$blue"

        return arrayOf(alpha, red, green, blue)
    }

    /**
     * Manages the custom brightness value subscription
     * @param enabled determines if the subscription get (un)subscribed
     */
    private fun setCustomBrightness(enabled: Boolean) {
        if (enabled) {
            preferences.customBrightnessValue().asFlow()
                .sample(100)
                .onEach { setCustomBrightnessValue(it) }
                .launchIn((context as ReaderActivity).lifecycleScope)
        } else {
            setCustomBrightnessValue(0, true)
        }
        setCustomBrightnessSeekBar(enabled)
    }

    /**
     * Sets the brightness of the screen. Range is [-75, 100].
     * From -75 to -1 a semi-transparent black view is shown at the top with the minimum brightness.
     * From 1 to 100 it sets that value as brightness.
     * 0 sets system brightness and hides the overlay.
     */
    private fun setCustomBrightnessValue(value: Int, isDisabled: Boolean = false) {
        if (!isDisabled) {
            binding.txtBrightnessSeekbarValue.text = value.toString()
        }
    }

    /**
     * Manages the color filter value subscription
     * @param enabled determines if the subscription get (un)subscribed
     */
    private fun setColorFilter(enabled: Boolean) {
        if (enabled) {
            preferences.colorFilterValue().asFlow()
                .sample(100)
                .onEach { setColorFilterValue(it) }
                .launchIn((context as ReaderActivity).lifecycleScope)
        }
        setColorFilterSeekBar(enabled)
    }

    /**
     * Sets the color filter overlay of the screen. Determined by HEX of integer
     * @param color hex of color.
     */
    private fun setColorFilterValue(@ColorInt color: Int) {
        setValues(color)
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
