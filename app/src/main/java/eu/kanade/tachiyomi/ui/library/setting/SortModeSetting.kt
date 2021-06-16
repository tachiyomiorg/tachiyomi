package eu.kanade.tachiyomi.ui.library.setting

import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.preference.PreferencesHelper

enum class SortModeSetting(val flag: Int) {
    ALPHABETICAL(0b00000000),
    LAST_READ(0b00000100),
    LAST_CHECKED(0b00001000),
    UNREAD(0b00001100),
    TOTAL_CHAPTERS(0b00010000),
    LATEST_CHAPTER(0b00010100),
    DATE_FETCHED(0b00011000),
    DATE_ADDED(0b00011100);

    companion object {
        // Mask supports for more sorting flags if necessary
        const val MASK = 0b00111100

        fun fromFlag(flag: Int?): SortModeSetting {
            return values().find { mode -> mode.flag == flag } ?: ALPHABETICAL
        }

        fun get(preferences: PreferencesHelper, category: Category?): SortModeSetting {
            return if (preferences.categorisedDisplaySettings().get() && category != null && category.id != 0) {
                fromFlag(category.sortMode)
            } else {
                preferences.librarySortingMode().get()
            }
        }
    }
}
