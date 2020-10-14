package eu.kanade.tachiyomi.data.backup.models

import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.SourceManager
import uy.kohesive.injekt.injectLazy

abstract class AbstractBackupManager(protected val context: Context) {
    internal val databaseHelper: DatabaseHelper by injectLazy()
    internal val sourceManager: SourceManager by injectLazy()
    internal val trackManager: TrackManager by injectLazy()
    protected val preferences: PreferencesHelper by injectLazy()

    abstract fun createBackup(uri: Uri, flags: Int, isJob: Boolean): String?

    /**
     * Returns manga
     *
     * @return [Manga], null if not found
     */
    internal fun getMangaFromDatabase(manga: Manga): Manga? =
        databaseHelper.getManga(manga.url, manga.source).executeAsBlocking()

    /**
     * Returns list containing manga from library
     *
     * @return [Manga] from library
     */
    protected fun getFavoriteManga(): List<Manga> =
        databaseHelper.getFavoriteMangas().executeAsBlocking()

    /**
     * Inserts manga and returns id
     *
     * @return id of [Manga], null if not found
     */
    internal fun insertManga(manga: Manga): Long? =
        databaseHelper.insertManga(manga).executeAsBlocking().insertedId()

    /**
     * Inserts list of chapters
     */
    protected fun insertChapters(chapters: List<Chapter>) {
        databaseHelper.insertChapters(chapters).executeAsBlocking()
    }

    /**
     * Updates a list of chapters
     */
    protected fun updateChapters(chapters: List<Chapter>) {
        databaseHelper.updateChaptersBackup(chapters).executeAsBlocking()
    }

    /**
     * Return number of backups.
     *
     * @return number of backups selected by user
     */
    protected fun numberOfBackups(): Int = preferences.numberOfBackups().get()
}
