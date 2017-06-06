package eu.kanade.tachiyomi

import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.data.updater.UpdateCheckerJob
import java.io.File

object Migrations {

    /**
     * Performs a migration when the application is updated.
     *
     * @param preferences Preferences of the application.
     * @return true if a migration is performed, false otherwise.
     */
    fun upgrade(preferences: PreferencesHelper): Boolean {
        val context = preferences.context
        val oldVersion = preferences.lastVersionCode().getOrDefault()
        if (oldVersion < BuildConfig.VERSION_CODE) {
            preferences.lastVersionCode().set(BuildConfig.VERSION_CODE)

            if (oldVersion == 0) return false

            if (oldVersion < 14) {
                // Restore jobs after upgrading to evernote's job scheduler.
                if (BuildConfig.INCLUDE_UPDATER && preferences.automaticUpdates()) {
                    UpdateCheckerJob.setupTask()
                }
                LibraryUpdateJob.setupTask()
            }
            if (oldVersion < 15) {
                // Delete internal chapter cache dir.
                File(context.cacheDir, "chapter_disk_cache").deleteRecursively()
            }
            if (oldVersion < 19) {
                // Move covers to external files dir.
                val oldDir = File(context.externalCacheDir, "cover_disk_cache")
                if (oldDir.exists()) {
                    val destDir = context.getExternalFilesDir("covers")
                    if (destDir != null) {
                        oldDir.listFiles().forEach {
                            it.renameTo(File(destDir, it.name))
                        }
                    }
                }
            }
            return true
        }
        return false
    }

}