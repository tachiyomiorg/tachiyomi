package eu.kanade.tachiyomi.ui.library.setting

import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.preference.PreferencesHelper

enum class SortDirectionSetting(val flag: Int) {
    ASCENDING(0b01000000),
    DESCENDING(0b00000000);

    companion object {
        const val MASK = 0b01000000

        fun fromFlag(flag: Int?): SortDirectionSetting {
            return values().find { mode -> mode.flag == flag } ?: ASCENDING
        }

        fun get(preferences: PreferencesHelper, category: Category?): SortDirectionSetting {
            return if (preferences.categorisedDisplaySettings().get() && category != null && category.id != 0) {
                fromFlag(category.sortDirection)
            } else {
                preferences.librarySortingAscending().get()
            }
        }
    }
}
