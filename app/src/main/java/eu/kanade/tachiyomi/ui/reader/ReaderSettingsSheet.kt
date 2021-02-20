package eu.kanade.tachiyomi.ui.reader

import android.os.Bundle
import android.widget.CompoundButton
import android.widget.Spinner
import androidx.annotation.ArrayRes
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.tfcporciuncula.flow.Preference
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.asImmediateFlow
import eu.kanade.tachiyomi.databinding.ReaderSettingsSheetBinding
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer
import eu.kanade.tachiyomi.widget.IgnoreFirstSpinnerListener
import eu.kanade.tachiyomi.widget.sheet.BaseBottomSheetDialog
import kotlinx.coroutines.flow.launchIn
import uy.kohesive.injekt.injectLazy

/**
 * Sheet to show reader and viewer preferences.
 */
class ReaderSettingsSheet(private val activity: ReaderActivity) : BaseBottomSheetDialog(activity) {

    private val preferences: PreferencesHelper by injectLazy()

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
        binding.dualPageSplit.bindToPreference(preferences.dualPageSplit())
        binding.keepscreen.bindToPreference(preferences.keepScreenOn())
        binding.longTap.bindToPreference(preferences.readWithLongTap())
        binding.alwaysShowChapterTransition.bindToPreference(preferences.alwaysShowChapterTransition())
        binding.pageTransitions.bindToPreference(preferences.pageTransitions())

        // Makes so that dual page invert gets hidden away when turning of dual page split
        preferences.dualPageSplit()
            .asImmediateFlow { binding.dualPageInvert.isVisible = it }
            .launchIn(activity.lifecycleScope)
        binding.dualPageInvert.bindToPreference(preferences.dualPageInvert())

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
        binding.webtoonPrefsGroup.root.isVisible = false
        binding.pagerPrefsGroup.root.isVisible = true

        binding.pagerPrefsGroup.tappingPrefsGroup.isVisible = preferences.readWithTapping().get()

        binding.pagerPrefsGroup.tappingInverted.bindToPreference(preferences.pagerNavInverted())

        binding.pagerPrefsGroup.pagerNav.bindToPreference(preferences.navigationModePager())
        binding.pagerPrefsGroup.scaleType.bindToPreference(preferences.imageScaleType(), 1)
        binding.pagerPrefsGroup.zoomStart.bindToPreference(preferences.zoomStart(), 1)
        binding.pagerPrefsGroup.cropBorders.bindToPreference(preferences.cropBorders())
    }

    /**
     * Init the preferences for the webtoon reader.
     */
    private fun initWebtoonPreferences() {
        binding.pagerPrefsGroup.root.isVisible = false
        binding.webtoonPrefsGroup.root.isVisible = true

        binding.webtoonPrefsGroup.tappingPrefsGroup.isVisible = preferences.readWithTapping().get()

        binding.webtoonPrefsGroup.tappingInverted.bindToPreference(preferences.webtoonNavInverted())

        binding.webtoonPrefsGroup.webtoonNav.bindToPreference(preferences.navigationModeWebtoon())
        binding.webtoonPrefsGroup.cropBordersWebtoon.bindToPreference(preferences.cropBordersWebtoon())
        binding.webtoonPrefsGroup.webtoonSidePadding.bindToIntPreference(preferences.webtoonSidePadding(), R.array.webtoon_side_padding_values)
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
