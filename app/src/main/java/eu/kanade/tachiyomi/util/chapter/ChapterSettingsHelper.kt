package eu.kanade.tachiyomi.util.chapter

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.lang.launchIO
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object ChapterSettingsHelper {
    private val prefs = Injekt.get<PreferencesHelper>()
    private val db: DatabaseHelper = Injekt.get()

    /**
     * compares manga Chapter Settings to the app's defaults
     */
    fun matchesSettingsDefaults(m: Manga): Boolean {
        val sort = if (m.sortDescending()) Manga.SORT_DESC else Manga.SORT_ASC

        return when {
            (
                m.readFilter == Manga.SHOW_ALL &&
                    m.downloadedFilter == Manga.SHOW_ALL &&
                    m.bookmarkedFilter == Manga.SHOW_ALL &&
                    m.sorting == Manga.SORTING_SOURCE &&
                    m.displayMode == Manga.DISPLAY_NAME &&
                    sort == Manga.SORT_DESC
                ) -> true
            else -> false
        }
    }

    /**
     * compares manga Chapter Settings to the Chapter Settings set in Preferences
     */
    fun matchesSettingsDefaultsFromPreferences(m: Manga): Boolean {
        val sort = if (m.sortDescending()) Manga.SORT_DESC else Manga.SORT_ASC

        return when {
            (
                m.readFilter == prefs.filterChapterByRead() &&
                    m.downloadedFilter == prefs.filterChapterByDownloaded() &&
                    m.bookmarkedFilter == prefs.filterChapterByBookmarked() &&
                    m.sorting == prefs.sortChapterBySourceOrNumber() &&
                    m.displayMode == prefs.displayChapterByNameOrNumber() &&
                    sort == prefs.sortChapterByAscendingOrDescending()
                ) -> true
            else -> false
        }
    }

    /**
     * updates the Chapter Settings in Preferences
     */
    fun setNewSettingDefaults(m: Manga) {
        prefs.setChapterSettingsDefault(m)
        db.updateFlags(m).executeAsBlocking()
    }

    /**
     * updates a single manga's Chapter Settings to match what's set in Preferences
     */
    fun applySettingDefaultsFromPreferences(m: Manga) {
        m.readFilter = prefs.filterChapterByRead()
        m.downloadedFilter = prefs.filterChapterByDownloaded()
        m.bookmarkedFilter = prefs.filterChapterByBookmarked()
        m.sorting = prefs.sortChapterBySourceOrNumber()
        m.displayMode = prefs.displayChapterByNameOrNumber()
        m.setChapterOrder(prefs.sortChapterByAscendingOrDescending())
        db.updateFlags(m).executeAsBlocking()
    }

    /**
     * updates all mangas in database Chapter Settings to match what's set in Preferences
     */
    fun updateAllMangasWithDefaultsFromPreferences() {
        launchIO {
            val dbMangas = db.getMangas().executeAsBlocking().toMutableList()

            val updatedMangas = dbMangas.map { m ->
                m.readFilter = prefs.filterChapterByRead()
                m.downloadedFilter = prefs.filterChapterByDownloaded()
                m.bookmarkedFilter = prefs.filterChapterByBookmarked()
                m.sorting = prefs.sortChapterBySourceOrNumber()
                m.displayMode = prefs.displayChapterByNameOrNumber()
                m.setChapterOrder(prefs.sortChapterByAscendingOrDescending())
                m
            }.toList()

            db.updateFlags(updatedMangas).executeAsBlocking()
        }
    }
}
