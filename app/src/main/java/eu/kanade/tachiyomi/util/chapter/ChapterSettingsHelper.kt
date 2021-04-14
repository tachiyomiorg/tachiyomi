package eu.kanade.tachiyomi.util.chapter

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.lang.launchIO
import uy.kohesive.injekt.injectLazy

object ChapterSettingsHelper {

    private val prefs: PreferencesHelper by injectLazy()
    private val db: DatabaseHelper by injectLazy()

    /**
     * Updates the global Chapter Settings in Preferences.
     */
    fun setGlobalSettings(manga: Manga?) {
        manga?.let {
            prefs.setChapterSettingsDefault(it)
            db.updateChapterFlags(it).executeAsBlocking()
            db.updateViewerFlags(it).executeAsBlocking()
        }
    }

    /**
     * Updates a single manga's Chapter Settings to match what's set in Preferences.
     */
    fun applySettingDefaults(manga: Manga) {
        with(manga) {
            readFilter = prefs.filterChapterByRead()
            downloadedFilter = prefs.filterChapterByDownloaded()
            bookmarkedFilter = prefs.filterChapterByBookmarked()
            sorting = prefs.sortChapterBySourceOrNumber()
            displayMode = prefs.displayChapterByNameOrNumber()
            setChapterOrder(prefs.sortChapterByAscendingOrDescending())
        }

        db.updateChapterFlags(manga).executeAsBlocking()
        db.updateViewerFlags(manga).executeAsBlocking()
    }

    /**
     * Updates all mangas in library with global Chapter Settings.
     */
    fun updateAllMangasWithGlobalDefaults() {
        launchIO {
            val updatedMangas = db.getMangas().executeAsBlocking().map { manga ->
                with(manga) {
                    readFilter = prefs.filterChapterByRead()
                    downloadedFilter = prefs.filterChapterByDownloaded()
                    bookmarkedFilter = prefs.filterChapterByBookmarked()
                    sorting = prefs.sortChapterBySourceOrNumber()
                    displayMode = prefs.displayChapterByNameOrNumber()
                    setChapterOrder(prefs.sortChapterByAscendingOrDescending())
                }
                manga
            }

            db.updateChapterFlags(updatedMangas).executeAsBlocking()
            db.updateViewerFlags(updatedMangas).executeAsBlocking()
        }
    }
}
