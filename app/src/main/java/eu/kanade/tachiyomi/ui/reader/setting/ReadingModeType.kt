package eu.kanade.tachiyomi.ui.reader.setting

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import eu.kanade.tachiyomi.R

enum class ReadingModeType(val prefValue: Int, @StringRes val stringRes: Int, @DrawableRes val iconRes: Int, val flagValue: Int) {
    DEFAULT(0, R.string.default_viewer, R.drawable.ic_reader_default_24dp, 0x00000000),
    LEFT_TO_RIGHT(1, R.string.left_to_right_viewer, R.drawable.ic_reader_ltr_24dp, 0x00000001),
    RIGHT_TO_LEFT(2, R.string.right_to_left_viewer, R.drawable.ic_reader_rtl_24dp, 0x00000002),
    VERTICAL(3, R.string.vertical_viewer, R.drawable.ic_reader_vertical_24dp, 0x00000003),
    WEBTOON(4, R.string.webtoon_viewer, R.drawable.ic_reader_webtoon_24dp, 0x00000004),
    CONTINUOUS_VERTICAL(5, R.string.vertical_plus_viewer, R.drawable.ic_reader_continuous_vertical_24dp, 0x00000005),
    ;

    companion object {
        const val MASK = 0x00000007

        fun fromPreference(preference: Int?): ReadingModeType = values().find { it.flagValue == preference } ?: DEFAULT

        fun isPagerType(preference: Int): Boolean {
            val mode = fromPreference(preference)
            return mode == LEFT_TO_RIGHT || mode == RIGHT_TO_LEFT || mode == VERTICAL
        }

        fun fromSpinner(position: Int?) = values().find { value -> value.prefValue == position } ?: DEFAULT
    }
}
