package eu.kanade.tachiyomi.ui.reader.setting

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.ReaderGeneralSettingsBinding
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.preference.bindToPreference
import uy.kohesive.injekt.injectLazy

/**
 * Sheet to show reader and viewer preferences.
 */
class ReaderGeneralSettings @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    NestedScrollView(context, attrs) {

    private val preferences: PreferencesHelper by injectLazy()

    private val binding = ReaderGeneralSettingsBinding.inflate(LayoutInflater.from(context), this, false)

    init {
        addView(binding.root)

        initGeneralPreferences()
    }

    /**
     * Init general reader preferences.
     */
    private fun initGeneralPreferences() {
        binding.backgroundColor.bindToIntPreference(preferences.readerTheme(), R.array.reader_themes_values)
        binding.showPageNumber.bindToPreference(preferences.showPageNumber())
        binding.fullscreen.bindToPreference(preferences.fullscreen())
        binding.keepscreen.bindToPreference(preferences.keepScreenOn())
        binding.longTap.bindToPreference(preferences.readWithLongTap())
        binding.alwaysShowChapterTransition.bindToPreference(preferences.alwaysShowChapterTransition())
        binding.pageTransitions.bindToPreference(preferences.pageTransitions())

        // If the preference is explicitly disabled, that means the setting was configured since there is a cutout
        if ((context as ReaderActivity).hasCutout || !preferences.cutoutShort().get()) {
            binding.cutoutShort.isVisible = true
            binding.cutoutShort.bindToPreference(preferences.cutoutShort())
        }
    }
}
