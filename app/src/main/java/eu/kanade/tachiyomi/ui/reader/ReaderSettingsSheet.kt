package eu.kanade.tachiyomi.ui.reader

import android.os.Bundle
import android.widget.CompoundButton
import android.widget.Spinner
import androidx.annotation.ArrayRes
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.plusAssign
import androidx.core.widget.NestedScrollView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tfcporciuncula.flow.Preference
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.ReaderSettingsSheetBinding
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer
import eu.kanade.tachiyomi.widget.IgnoreFirstSpinnerListener
import uy.kohesive.injekt.injectLazy

/**
 * Sheet to show reader and viewer preferences.
 */
class ReaderSettingsSheet(private val activity: ReaderActivity) : BottomSheetDialog(activity) {

    private val preferences by injectLazy<PreferencesHelper>()

    private val binding = ReaderSettingsSheetBinding.inflate(activity.layoutInflater, null, false)

    init {
        val scroll = NestedScrollView(activity)
        scroll.addView(binding.root)
        setContentView(scroll)
    }

    /**
     * Called when the sheet is created. It initializes the listeners and values of the preferences.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initGeneralPreferences()
        initNavigationPreferences()

        when (activity.viewer) {
            is PagerViewer -> initPagerPreferences()
            is WebtoonViewer -> initWebtoonPreferences()
        }
    }

    /**
     * Init general reader preferences.
     */
    private fun initGeneralPreferences() {
        binding.viewer.onItemSelectedListener = IgnoreFirstSpinnerListener { position ->
            activity.presenter.setMangaViewer(position)

            val mangaViewer = activity.presenter.getMangaViewer()
            if (mangaViewer == ReaderActivity.WEBTOON || mangaViewer == ReaderActivity.VERTICAL_PLUS) {
                initWebtoonPreferences()
            } else {
                initPagerPreferences()
            }
        }
        binding.viewer.setSelection(activity.presenter.manga?.viewer ?: 0, false)

        binding.rotationMode.bindToPreference(preferences.rotation(), 1)
        binding.backgroundColor.bindToIntPreference(preferences.readerTheme(), R.array.reader_themes_values)
        binding.showPageNumber.bindToPreference(preferences.showPageNumber())
        binding.fullscreen.bindToPreference(preferences.fullscreen())
        binding.keepscreen.bindToPreference(preferences.keepScreenOn())
        binding.longTap.bindToPreference(preferences.readWithLongTap())
        binding.alwaysShowChapterTransition.bindToPreference(preferences.alwaysShowChapterTransition())
        binding.pageTransitions.bindToPreference(preferences.pageTransitions())

        // If the preference is explicitly disabled, that means the setting was configured since there is a cutout
        if (activity.hasCutout || !preferences.cutoutShort().get()) {
            binding.cutoutShort.isVisible = true
            binding.cutoutShort.bindToPreference(preferences.cutoutShort())
        }
    }

    /**
     * Init the preferences for the pager reader.
     */
    private fun initPagerPreferences() {
        binding.webtoonPrefsGroup.isInvisible = true
        binding.pagerPrefsGroup.isVisible = true

        binding.scaleType.bindToPreference(preferences.imageScaleType(), 1)
        binding.zoomStart.bindToPreference(preferences.zoomStart(), 1)
        binding.cropBorders.bindToPreference(preferences.cropBorders())
    }

    /**
     * Init the preferences for the webtoon reader.
     */
    private fun initWebtoonPreferences() {
        binding.pagerPrefsGroup.isInvisible = true
        binding.webtoonPrefsGroup.isVisible = true

        binding.cropBordersWebtoon.bindToPreference(preferences.cropBordersWebtoon())
        binding.webtoonSidePadding.bindToIntPreference(preferences.webtoonSidePadding(), R.array.webtoon_side_padding_values)
    }

    /**
     * Init the preferences for navigation.
     */
    private fun initNavigationPreferences() {
        if (!preferences.readWithTapping().get()) {
            binding.navigationPrefsGroup.isVisible = false
        }

        binding.tappingInverted.bindToPreference(preferences.readWithTappingInverted())
    }

    /**
     * Binds a checkbox or switch view with a boolean preference.
     */
    private fun CompoundButton.bindToPreference(pref: Preference<Boolean>) {
        isChecked = pref.get()
        setOnCheckedChangeListener { _, isChecked -> pref.set(isChecked) }
    }

    /**
     * Binds a spinner to an int preference with an optional offset for the value.
     */
    private fun Spinner.bindToPreference(pref: Preference<Int>, offset: Int = 0) {
        onItemSelectedListener = IgnoreFirstSpinnerListener { position ->
            pref.set(position + offset)
        }
        setSelection(pref.get() - offset, false)
    }

    /**
     * Binds a spinner to an enum preference.
     */
    private inline fun <reified T : Enum<T>> Spinner.bindToPreference(pref: Preference<T>) {
        val enumConstants = T::class.java.enumConstants

        onItemSelectedListener = IgnoreFirstSpinnerListener { position ->
            enumConstants?.get(position)?.let { pref.set(it) }
        }

        enumConstants?.indexOf(pref.get())?.let { setSelection(it, false) }
    }

    /**
     * Binds a spinner to an int preference. The position of the spinner item must
     * correlate with the [intValues] resource item (in arrays.xml), which is a <string-array>
     * of int values that will be parsed here and applied to the preference.
     */
    private fun Spinner.bindToIntPreference(pref: Preference<Int>, @ArrayRes intValuesResource: Int) {
        val intValues = resources.getStringArray(intValuesResource).map { it.toIntOrNull() }
        onItemSelectedListener = IgnoreFirstSpinnerListener { position ->
            pref.set(intValues[position]!!)
        }
        setSelection(intValues.indexOf(pref.get()), false)
    }
}
