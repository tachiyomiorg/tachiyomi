package eu.kanade.tachiyomi.ui.reader.setting

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.lang.next

enum class ReadingModeType(val prefValue: Int, @StringRes val stringRes: Int, @DrawableRes val iconRes: Int) {
    DEFAULT(0, R.string.default_viewer, R.drawable.ic_reader_default_24dp),
    LEFT_TO_RIGHT(1, R.string.left_to_right_viewer, R.drawable.ic_reader_ltr_24dp),
    RIGHT_TO_LEFT(2, R.string.right_to_left_viewer, R.drawable.ic_reader_rtl_24dp),
    VERTICAL(3, R.string.vertical_viewer, R.drawable.ic_reader_vertical_24dp),
    WEBTOON(4, R.string.webtoon_viewer, R.drawable.ic_reader_webtoon_24dp),
    CONTINUOUS_VERTICAL(5, R.string.vertical_plus_viewer, R.drawable.ic_reader_continuous_vertical_24dp),
    ;

    companion object {
        fun fromPreference(preference: Int): ReadingModeType = values().find { it.prefValue == preference } ?: DEFAULT

        fun getNextReadingMode(preference: Int): ReadingModeType {
            val current = fromPreference(preference)
            return current.next()
        }

        fun isPagerType(preference: Int): Boolean {
            val mode = fromPreference(preference)
            return mode == LEFT_TO_RIGHT || mode == RIGHT_TO_LEFT || mode == VERTICAL
        }
    }
}
