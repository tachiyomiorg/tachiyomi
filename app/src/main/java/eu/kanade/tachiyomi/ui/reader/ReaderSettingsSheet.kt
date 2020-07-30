package eu.kanade.tachiyomi.ui.reader

import android.os.Bundle
import android.widget.CompoundButton
import android.widget.Spinner
import androidx.annotation.ArrayRes
import androidx.core.widget.NestedScrollView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tfcporciuncula.flow.Preference
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.ReaderSettingsSheetBinding
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer
import eu.kanade.tachiyomi.util.view.gone
import eu.kanade.tachiyomi.util.view.invisible
import eu.kanade.tachiyomi.util.view.visible
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
            activity.presenter.setMangaReadingMode(position.let(::positionToReadingMode))

            val mangaViewer = activity.presenter.getMangaReadingMode()
            if (mangaViewer == Manga.READING_WEBTOON || mangaViewer == Manga.READING_CONT_VERTICAL) {
                initWebtoonPreferences()
            } else {
                initPagerPreferences()
            }
        }
        binding.viewer.setSelection(activity.presenter.manga?.readingMode.let(::readingModeToPosition), false)

        binding.rotationType.onItemSelectedListener = IgnoreFirstSpinnerListener { position ->
            activity.presenter.setMangaRotationType(position.let(::positionToRotationType))
            activity.setOrientation(activity.presenter.getMangaRotationType())
        }
        binding.rotationType.setSelection(activity.presenter.manga?.rotationType.let(::rotationTypeToPosition), false)

        binding.backgroundColor.bindToIntPreference(preferences.readerTheme(), R.array.reader_themes_values)
        binding.showPageNumber.bindToPreference(preferences.showPageNumber())
        binding.fullscreen.bindToPreference(preferences.fullscreen())
        binding.cutoutShort.bindToPreference(preferences.cutoutShort())
        binding.keepscreen.bindToPreference(preferences.keepScreenOn())
        binding.longTap.bindToPreference(preferences.readWithLongTap())
        binding.alwaysShowChapterTransition.bindToPreference(preferences.alwaysShowChapterTransition())
        binding.cropBorders.bindToPreference(preferences.cropBorders())
        binding.pageTransitions.bindToPreference(preferences.pageTransitions())
    }

    /**
     * Init the preferences for the pager reader.
     */
    private fun initPagerPreferences() {
        binding.webtoonPrefsGroup.invisible()
        binding.pagerPrefsGroup.visible()

        binding.scaleType.bindToPreference(preferences.imageScaleType(), 1)
        binding.zoomStart.bindToPreference(preferences.zoomStart(), 1)
    }

    /**
     * Init the preferences for the webtoon reader.
     */
    private fun initWebtoonPreferences() {
        binding.pagerPrefsGroup.invisible()
        binding.webtoonPrefsGroup.visible()

        binding.webtoonSidePadding.bindToIntPreference(preferences.webtoonSidePadding(), R.array.webtoon_side_padding_values)
    }

    /**
     * Init the preferences for navigation.
     */
    private fun initNavigationPreferences() {
        if (!preferences.readWithTapping().get()) {
            binding.navigationPrefsGroup.gone()
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
     * correlate with the [intValuesResource] resource item (in arrays.xml), which is a <string-array>
     * of int values that will be parsed here and applied to the preference.
     */
    private fun Spinner.bindToIntPreference(pref: Preference<Int>, @ArrayRes intValuesResource: Int) {
        val intValues = resources.getStringArray(intValuesResource).map { it.toIntOrNull() }
        onItemSelectedListener = IgnoreFirstSpinnerListener { position ->
            pref.set(intValues[position]!!)
        }
        setSelection(intValues.indexOf(pref.get()), false)
    }

    /**
     * Map from [position] of [binding#rotationType] to rotation type value.
     * @throws IllegalArgumentException if selected value is invalid
     * @see Manga.ROTATION_DEFAULT
     * @see Manga.ROTATION_FREE
     * @see Manga.ROTATION_LOCK
     * @see Manga.ROTATION_FORCE_PORTRAIT
     * @see Manga.ROTATION_FORCE_LANDSCAPE
     */
    private fun positionToRotationType(position: Int) = when (position) {
        0 -> Manga.ROTATION_DEFAULT
        1 -> Manga.ROTATION_FREE
        2 -> Manga.ROTATION_LOCK
        3 -> Manga.ROTATION_FORCE_PORTRAIT
        4 -> Manga.ROTATION_FORCE_LANDSCAPE
        else -> throw IllegalArgumentException() // should not happen
    }

    /**
     * Map from [position] of [binding#viewer] to rotation type value.
     * @throws IllegalArgumentException if selected value is invalid
     * @see Manga.READING_DEFAULT
     * @see Manga.READING_L2R
     * @see Manga.READING_R2L
     * @see Manga.READING_VERTICAL
     * @see Manga.READING_WEBTOON
     * @see Manga.READING_CONT_VERTICAL
     */
    private fun positionToReadingMode(position: Int) = when (position) {
        0 -> Manga.READING_DEFAULT
        1 -> Manga.READING_L2R
        2 -> Manga.READING_R2L
        3 -> Manga.READING_VERTICAL
        4 -> Manga.READING_WEBTOON
        5 -> Manga.READING_CONT_VERTICAL
        else -> throw IllegalArgumentException() // should not happen
    }

    /**
     * Map from [rotationType] value to position of [binding#rotationType] spinner.
     * Return 0 if invalid value.
     * @see Manga.ROTATION_DEFAULT
     * @see Manga.ROTATION_FREE
     * @see Manga.ROTATION_LOCK
     * @see Manga.ROTATION_FORCE_PORTRAIT
     * @see Manga.ROTATION_FORCE_LANDSCAPE
     */
    private fun rotationTypeToPosition(rotationType: Int?) = when (rotationType) {
        Manga.ROTATION_DEFAULT -> 0
        Manga.ROTATION_FREE -> 1
        Manga.ROTATION_LOCK -> 2
        Manga.ROTATION_FORCE_PORTRAIT -> 3
        Manga.ROTATION_FORCE_LANDSCAPE -> 4
        else -> 0
    }

    /**
     * Map from [readingMode] value to position of [binding#viewer] spinner.
     * Return 0 if invalid value.
     * @see Manga.READING_DEFAULT
     * @see Manga.READING_L2R
     * @see Manga.READING_R2L
     * @see Manga.READING_VERTICAL
     * @see Manga.READING_WEBTOON
     * @see Manga.READING_CONT_VERTICAL
     */
    private fun readingModeToPosition(readingMode: Int?) = when (readingMode) {
        Manga.READING_DEFAULT -> 0
        Manga.READING_L2R -> 1
        Manga.READING_R2L -> 2
        Manga.READING_VERTICAL -> 3
        Manga.READING_WEBTOON -> 4
        Manga.READING_CONT_VERTICAL -> 5
        else -> 0
    }
}
