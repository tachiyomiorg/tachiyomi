package eu.kanade.tachiyomi.data.backup

import android.content.Context
import com.github.salomonbrys.kotson.*
import com.google.gson.*
import eu.kanade.tachiyomi.data.backup.BackupCreateService.Companion.BACKUP_CATEGORY
import eu.kanade.tachiyomi.data.backup.BackupCreateService.Companion.BACKUP_CATEGORY_MASK
import eu.kanade.tachiyomi.data.backup.BackupCreateService.Companion.BACKUP_CHAPTER
import eu.kanade.tachiyomi.data.backup.BackupCreateService.Companion.BACKUP_CHAPTER_MASK
import eu.kanade.tachiyomi.data.backup.BackupCreateService.Companion.BACKUP_HISTORY
import eu.kanade.tachiyomi.data.backup.BackupCreateService.Companion.BACKUP_HISTORY_MASK
import eu.kanade.tachiyomi.data.backup.BackupCreateService.Companion.BACKUP_TRACK
import eu.kanade.tachiyomi.data.backup.BackupCreateService.Companion.BACKUP_TRACK_MASK
import eu.kanade.tachiyomi.data.backup.models.Backup.CATEGORIES
import eu.kanade.tachiyomi.data.backup.models.Backup.CHAPTERS
import eu.kanade.tachiyomi.data.backup.models.Backup.CURRENT_VERSION
import eu.kanade.tachiyomi.data.backup.models.Backup.HISTORY
import eu.kanade.tachiyomi.data.backup.models.Backup.MANGA
import eu.kanade.tachiyomi.data.backup.models.Backup.TRACK
import eu.kanade.tachiyomi.data.backup.models.DHistory
import eu.kanade.tachiyomi.data.backup.serializer.*
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.*
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.util.syncChaptersWithSource
import rx.Observable
import uy.kohesive.injekt.injectLazy
import java.util.*

class BackupManager(val context: Context, version: Int = CURRENT_VERSION) {

    /**
     * Database.
     */
    internal val databaseHelper: DatabaseHelper by injectLazy()

    /**
     * Source manager.
     */
    internal val sourceManager: SourceManager by injectLazy()

    /**
     * Version of parser
     */
    var version: Int = version
        private set

    /**
     * Json Parser
     */
    var parser: Gson = initParser()

    /**
     * Preferences
     */
    private val preferences: PreferencesHelper by injectLazy()

    /**
     * Set version of parser
     *
     * @param version version of parser
     */
    internal fun setVersion(version: Int) {
        this.version = version
        parser = initParser()
    }

    private fun initParser(): Gson {
        return when (version) {
            1 -> GsonBuilder().create()
            2 -> GsonBuilder()
                    .registerTypeAdapter<MangaImpl>(MangaTypeAdapter.build())
                    .registerTypeHierarchyAdapter<ChapterImpl>(ChapterTypeAdapter.build())
                    .registerTypeAdapter<CategoryImpl>(CategoryTypeAdapter.build())
                    .registerTypeAdapter<DHistory>(HistoryTypeAdapter.build())
                    .registerTypeHierarchyAdapter<TrackImpl>(TrackTypeAdapter.build())
                    .create()
            else -> throw Exception("Json version unknown")
        }
    }

    /**
     * Backup the categories of library
     *
     * @param root root of categories json
     */
    internal fun backupCategories(root: JsonArray) {
        val categories = databaseHelper.getCategories().executeAsBlocking()
        categories.forEach { root.add(parser.toJsonTree(it)) }
    }

    /**
     * Convert a manga to Json
     *
     * @param manga manga that gets converted
     * @return [JsonElement] containing manga information
     */
    internal fun backupMangaObject(manga: Manga, options: Int): JsonElement {
        // Entry for this manga
        val entry = JsonObject()

        // Backup manga fields
        entry[MANGA] = parser.toJsonTree(manga)

        // Check if user wants chapter information in backup
        if (options and BACKUP_CHAPTER_MASK == BACKUP_CHAPTER) {
            // Backup all the chapters
            val chapters = databaseHelper.getChapters(manga).executeAsBlocking()
            if (!chapters.isEmpty()) {
                val chaptersJson = parser.toJsonTree(chapters)
                if (chaptersJson.asJsonArray.size() > 0) {
                    entry[CHAPTERS] = chaptersJson
                }
            }
        }

        // Check if user wants category information in backup
        if (options and BACKUP_CATEGORY_MASK == BACKUP_CATEGORY) {
            // Backup categories for this manga
            val categoriesForManga = databaseHelper.getCategoriesForManga(manga).executeAsBlocking()
            if (!categoriesForManga.isEmpty()) {
                val categoriesNames = categoriesForManga.map { it.name }
                entry[CATEGORIES] = parser.toJsonTree(categoriesNames)
            }
        }

        // Check if user wants track information in backup
        if (options and BACKUP_TRACK_MASK == BACKUP_TRACK) {
            val tracks = databaseHelper.getTracks(manga).executeAsBlocking()
            if (!tracks.isEmpty()) {
                entry[TRACK] = parser.toJsonTree(tracks)
            }
        }

        // Check if user wants history information in backup
        if (options and BACKUP_HISTORY_MASK == BACKUP_HISTORY) {
            val historyForManga = databaseHelper.getHistoryByMangaId(manga.id!!).executeAsBlocking()
            if (!historyForManga.isEmpty()) {
                val historyData = historyForManga.mapNotNull { history ->
                    val url = databaseHelper.getChapter(history.chapter_id).executeAsBlocking()?.url
                    url?.let { DHistory(url, history.last_read) }
                }
                val historyJson = parser.toJsonTree(historyData)
                if (historyJson.asJsonArray.size() > 0) {
                    entry[HISTORY] = historyJson
                }
            }
        }

        return entry
    }

    fun restoreMangaNoFetch(manga: Manga, dbManga: Manga) {
        manga.id = dbManga.id
        manga.copyFrom(dbManga)
        manga.favorite = true
        insertManga(manga)
    }

    /**
     * [Observable] that fetches manga information
     *
     * @param source source of manga
     * @param manga manga that needs updating
     * @return [Observable] that contains manga
     */
    fun restoreMangaFetchObservable(source: Source, manga: Manga): Observable<Manga> {
        return source.fetchMangaDetails(manga)
                .map { networkManga ->
                    manga.copyFrom(networkManga)
                    manga.favorite = true
                    manga.initialized = true
                    manga.id = insertManga(manga)
                    manga
                }
    }

    /**
     * [Observable] that fetches chapter information
     *
     * @param source source of manga
     * @param manga manga that needs updating
     * @return [Observable] that contains manga
     */
    fun restoreChapterFetchObservable(source: Source, manga: Manga, chapters: List<Chapter>): Observable<Pair<List<Chapter>, List<Chapter>>> {
        return source.fetchChapterList(manga)
                .map { syncChaptersWithSource(databaseHelper, it, manga, source) }
                .doOnNext {
                    if (it.first.isNotEmpty()) {
                        chapters.forEach { it.manga_id = manga.id }
                        insertChapters(chapters)
                    }
                }
    }

    /**
     * Restore the categories from Json
     *
     * @param jsonCategories array containing categories
     */
    internal fun restoreCategories(jsonCategories: JsonArray) {
        // Get categories from file and from db
        val dbCategories = databaseHelper.getCategories().executeAsBlocking()
        val backupCategories = parser.fromJson<List<CategoryImpl>>(jsonCategories)

        // Iterate over them
        backupCategories.forEach { category ->
            // Used to know if the category is already in the db
            var found = false
            for (dbCategory in dbCategories) {
                // If the category is already in the db, assign the id to the file's category
                // and do nothing
                if (category.nameLower == dbCategory.nameLower) {
                    category.id = dbCategory.id
                    found = true
                    break
                }
            }
            // If the category isn't in the db, remove the id and insert a new category
            // Store the inserted id in the category
            if (!found) {
                // Let the db assign the id
                category.id = null
                val result = databaseHelper.insertCategory(category).executeAsBlocking()
                category.id = result.insertedId()?.toInt()
            }
        }
    }

    /**
     * Restores the categories a manga is in.
     *
     * @param manga the manga whose categories have to be restored.
     * @param categories the categories to restore.
     */
    internal fun restoreCategoriesForManga(manga: Manga, categories: List<String>) {
        val dbCategories = databaseHelper.getCategories().executeAsBlocking()
        val mangaCategoriesToUpdate = ArrayList<MangaCategory>()
        for (backupCategoryStr in categories) {
            for (dbCategory in dbCategories) {
                if (backupCategoryStr.toLowerCase() == dbCategory.nameLower) {
                    mangaCategoriesToUpdate.add(MangaCategory.create(manga, dbCategory))
                    break
                }
            }
        }

        // Update database
        if (!mangaCategoriesToUpdate.isEmpty()) {
            val mangaAsList = ArrayList<Manga>()
            mangaAsList.add(manga)
            databaseHelper.deleteOldMangasCategories(mangaAsList).executeAsBlocking()
            databaseHelper.insertMangasCategories(mangaCategoriesToUpdate).executeAsBlocking()
        }
    }

    /**
     * Restore history from Json
     *
     * @param history list containing history to be restored
     */
    internal fun restoreHistoryForManga(history: List<DHistory>) {
        // List containing history to be updated
        val historyToBeUpdated = ArrayList<History>()
        for ((url, lastRead) in history) {
            val dbHistory = databaseHelper.getHistoryByChapterUrl(url).executeAsBlocking()
            // Check if history already in database and update
            if (dbHistory != null) {
                dbHistory.apply {
                    last_read = Math.max(lastRead, dbHistory.last_read)
                }
                historyToBeUpdated.add(dbHistory)
            } else {
                // If not in database create
                databaseHelper.getChapter(url).executeAsBlocking()?.let {
                    val historyToAdd = History.create(it).apply {
                        last_read = lastRead
                    }
                    historyToBeUpdated.add(historyToAdd)
                }
            }
        }
        databaseHelper.updateHistoryLastRead(historyToBeUpdated).executeAsBlocking()
    }

    /**
     * Restores the sync of a manga.
     *
     * @param manga the manga whose sync have to be restored.
     * @param tracks the track list to restore.
     */
    internal fun restoreTrackForManga(manga: Manga, tracks: List<Track>) {
        // Fix foreign keys with the current manga id
        tracks.map { it.manga_id = manga.id!! }

        // Get tracks from database
        val dbTracks = databaseHelper.getTracks(manga).executeAsBlocking()
        val trackToUpdate = ArrayList<Track>()

        for (track in tracks) {
            var isInDatabase = false
            for (dbTrack in dbTracks) {
                if (track.sync_id == dbTrack.sync_id) {
                    // The sync is already in the db, only update its fields
                    if (track.remote_id != dbTrack.remote_id) {
                        dbTrack.remote_id = track.remote_id
                    }
                    dbTrack.last_chapter_read = Math.max(dbTrack.last_chapter_read, track.last_chapter_read)
                    isInDatabase = true
                    trackToUpdate.add(dbTrack)
                    break
                }
            }
            if (!isInDatabase) {
                // Insert new sync. Let the db assign the id
                track.id = null
                trackToUpdate.add(track)
            }
        }
        // Update database
        if (!trackToUpdate.isEmpty()) {
            databaseHelper.insertTracks(trackToUpdate).executeAsBlocking()
        }
    }

    /**
     * Restore the chapters for manga if chapters already in database
     *
     * @param manga manga of chapters
     * @param chapters list containing chapters that get restored
     * @return boolean answering if chapter fetch is not needed
     */
    internal fun restoreChaptersForManga(manga: Manga, chapters: List<Chapter>): Boolean {
        val dbChapters = databaseHelper.getChapters(manga).executeAsBlocking()

        // Return if fetch is needed
        if (dbChapters.isEmpty() || dbChapters.size < chapters.size)
            return false

        for (chapter in chapters) {
            val pos = dbChapters.indexOf(chapter)
            if (pos != -1) {
                val dbChapter = dbChapters[pos]
                chapter.id = dbChapter.id
                chapter.copyFrom(dbChapter)
                break
            }
        }
        // Filter the chapters that couldn't be found.
        chapters.filter { it.id != null }
        chapters.map { it.manga_id = manga.id }

        insertChapters(chapters)
        return true
    }

    /**
     * Returns manga
     *
     * @return [Manga], null if not found
     */
    internal fun getMangaFromDatabase(manga: Manga): Manga? {
        return databaseHelper.getManga(manga.url, manga.source).executeAsBlocking()
    }

    /**
     * Returns list containing manga from library
     *
     * @return [Manga] from library
     */
    internal fun getFavoriteManga(): List<Manga> {
        return databaseHelper.getFavoriteMangas().executeAsBlocking()
    }

    /**
     * Inserts manga and returns id
     *
     * @return id of [Manga], null if not found
     */
    internal fun insertManga(manga: Manga): Long? {
        return databaseHelper.insertManga(manga).executeAsBlocking().insertedId()
    }

    /**
     * Inserts list of chapters
     */
    internal fun insertChapters(chapters: List<Chapter>) {
        databaseHelper.updateChaptersBackup(chapters).executeAsBlocking()
    }

    /**
     * Return number of backups.
     *
     * @return number of backups selected by user
     */
    fun numberOfBackups(): Int {
        return preferences.numberOfBackups().getOrDefault()
    }
}
