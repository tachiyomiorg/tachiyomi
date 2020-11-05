package eu.kanade.tachiyomi.data.backup.legacy

import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.data.backup.AbstractBackupManager
import eu.kanade.tachiyomi.data.backup.legacy.models.Backup.Companion.CURRENT_VERSION
import eu.kanade.tachiyomi.data.backup.legacy.models.DHistory
import eu.kanade.tachiyomi.data.backup.legacy.serializer.CategoryImplTypeSerializer
import eu.kanade.tachiyomi.data.backup.legacy.serializer.CategoryTypeSerializer
import eu.kanade.tachiyomi.data.backup.legacy.serializer.ChapterImplTypeSerializer
import eu.kanade.tachiyomi.data.backup.legacy.serializer.ChapterTypeSerializer
import eu.kanade.tachiyomi.data.backup.legacy.serializer.HistoryTypeSerializer
import eu.kanade.tachiyomi.data.backup.legacy.serializer.MangaImplTypeSerializer
import eu.kanade.tachiyomi.data.backup.legacy.serializer.MangaTypeSerializer
import eu.kanade.tachiyomi.data.backup.legacy.serializer.TrackImplTypeSerializer
import eu.kanade.tachiyomi.data.backup.legacy.serializer.TrackTypeSerializer
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.database.models.toMangaInfo
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.toSManga
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlin.math.max

class LegacyBackupManager(context: Context, version: Int = CURRENT_VERSION) : AbstractBackupManager(context) {

    val parser: Json = when (version) {
        2 -> Json {
            // Forks may have added items to backup
            ignoreUnknownKeys = true

            // Register custom serializers
            serializersModule = SerializersModule {
                contextual(MangaTypeSerializer)
                contextual(MangaImplTypeSerializer)
                contextual(ChapterTypeSerializer)
                contextual(ChapterImplTypeSerializer)
                contextual(CategoryTypeSerializer)
                contextual(CategoryImplTypeSerializer)
                contextual(TrackTypeSerializer)
                contextual(TrackImplTypeSerializer)
                contextual(HistoryTypeSerializer)
            }
        }
        else -> throw Exception("Unknown backup version")
    }

    /**
     * Create backup Json file from database
     *
     * @param uri path of Uri
     * @param isJob backup called from job
     */
    override fun createBackup(uri: Uri, flags: Int, isJob: Boolean) =
        throw IllegalStateException("Legacy backup creation is not supported")

    fun restoreMangaNoFetch(manga: Manga, dbManga: Manga) {
        manga.id = dbManga.id
        manga.copyFrom(dbManga)
        manga.favorite = true
        insertManga(manga)
    }

    /**
     * Fetches manga information
     *
     * @param source source of manga
     * @param manga manga that needs updating
     * @return Updated manga.
     */
    suspend fun fetchManga(source: Source, manga: Manga): Manga {
        val networkManga = source.getMangaDetails(manga.toMangaInfo())
        return manga.also {
            it.copyFrom(networkManga.toSManga())
            it.favorite = true
            it.initialized = true
            it.id = insertManga(manga)
        }
    }

    /**
     * Restore the categories from Json
     *
     * @param backupCategories array containing categories
     */
    internal fun restoreCategories(backupCategories: List<Category>) {
        // Get categories from file and from db
        val dbCategories = databaseHelper.getCategories().executeAsBlocking()

        // Iterate over them
        backupCategories.forEach { category ->
            // Used to know if the category is already in the db
            var found = false
            for (dbCategory in dbCategories) {
                // If the category is already in the db, assign the id to the file's category
                // and do nothing
                if (category.name == dbCategory.name) {
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
        val mangaCategoriesToUpdate = ArrayList<MangaCategory>(categories.size)
        for (backupCategoryStr in categories) {
            for (dbCategory in dbCategories) {
                if (backupCategoryStr == dbCategory.name) {
                    mangaCategoriesToUpdate.add(MangaCategory.create(manga, dbCategory))
                    break
                }
            }
        }

        // Update database
        if (mangaCategoriesToUpdate.isNotEmpty()) {
            databaseHelper.deleteOldMangasCategories(listOf(manga)).executeAsBlocking()
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
        val historyToBeUpdated = ArrayList<History>(history.size)
        for ((url, lastRead) in history) {
            val dbHistory = databaseHelper.getHistoryByChapterUrl(url).executeAsBlocking()
            // Check if history already in database and update
            if (dbHistory != null) {
                dbHistory.apply {
                    last_read = max(lastRead, dbHistory.last_read)
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
        // Get tracks from database
        val dbTracks = databaseHelper.getTracks(manga).executeAsBlocking()
        val trackToUpdate = ArrayList<Track>(tracks.size)

        tracks.forEach { track ->
            // Fix foreign keys with the current manga id
            track.manga_id = manga.id!!

            val service = trackManager.getService(track.sync_id)
            if (service != null && service.isLogged) {
                var isInDatabase = false
                for (dbTrack in dbTracks) {
                    if (track.sync_id == dbTrack.sync_id) {
                        // The sync is already in the db, only update its fields
                        if (track.media_id != dbTrack.media_id) {
                            dbTrack.media_id = track.media_id
                        }
                        if (track.library_id != dbTrack.library_id) {
                            dbTrack.library_id = track.library_id
                        }
                        dbTrack.last_chapter_read = max(dbTrack.last_chapter_read, track.last_chapter_read)
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
        }
        // Update database
        if (trackToUpdate.isNotEmpty()) {
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
        if (dbChapters.isEmpty() || dbChapters.size < chapters.size) {
            return false
        }

        for (chapter in chapters) {
            val pos = dbChapters.indexOf(chapter)
            if (pos != -1) {
                val dbChapter = dbChapters[pos]
                chapter.id = dbChapter.id
                chapter.copyFrom(dbChapter)
                break
            }

            chapter.manga_id = manga.id
        }

        // Filter the chapters that couldn't be found.
        updateChapters(chapters.filter { it.id != null })

        return true
    }
}
