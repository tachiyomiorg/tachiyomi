package eu.kanade.tachiyomi.data.backup

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.*
import com.google.gson.stream.JsonReader
import eu.kanade.tachiyomi.data.backup.serializer.BooleanSerializer
import eu.kanade.tachiyomi.data.backup.serializer.IdExclusion
import eu.kanade.tachiyomi.data.backup.serializer.IntegerSerializer
import eu.kanade.tachiyomi.data.backup.serializer.LongSerializer
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.*
import java.io.*
import java.util.*

/**
 * This class provides the necessary methods to create and restore backups for the data of the
 * application. The backup follows a JSON structure, with the following scheme:
 *
 * {
 *     "mangas": [
 *         {
 *             "manga": {"id": 1, ...},
 *             "chapters": [{"id": 1, ...}, {...}],
 *             "sync": [{"id": 1, ...}, {...}],
 *             "categories": ["cat1", "cat2", ...]
 *         },
 *         { ... }
 *     ],
 *     "categories": [
 *         {"id": 1, ...},
 *         {"id": 2, ...}
 *     ]
 * }
 *
 * @param db the database helper.
 */
class BackupManager(private val db: DatabaseHelper) {

    private val MANGA = "manga"
    private val MANGAS = "mangas"
    private val CHAPTERS = "chapters"
    private val TRACK = "sync"
    private val CATEGORIES = "categories"

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private val gson = GsonBuilder()
            .registerTypeAdapter(java.lang.Integer::class.java, IntegerSerializer())
            .registerTypeAdapter(java.lang.Boolean::class.java, BooleanSerializer())
            .registerTypeAdapter(java.lang.Long::class.java, LongSerializer())
            .setExclusionStrategies(IdExclusion())
            .create()

    /**
     * Backups the data of the application to a file.
     *
     * @param file the file where the backup will be saved.
     * @throws IOException if there's any IO error.
     */
    @Throws(IOException::class)
    fun backupToFile(file: File) {
        val root = backupToJson()

        FileWriter(file).use {
            gson.toJson(root, it)
        }
    }

    /**
     * Creates a JSON object containing the backup of the app's data.
     *
     * @return the backup as a JSON object.
     */
    fun backupToJson(): JsonObject {
        val root = JsonObject()

        // Backup library mangas and its dependencies
        val mangaEntries = JsonArray()
        root.add(MANGAS, mangaEntries)
        for (manga in db.getFavoriteMangas().executeAsBlocking()) {
            mangaEntries.add(backupManga(manga))
        }

        // Backup categories
        val categoryEntries = JsonArray()
        root.add(CATEGORIES, categoryEntries)
        for (category in db.getCategories().executeAsBlocking()) {
            categoryEntries.add(backupCategory(category))
        }

        return root
    }

    /**
     * Backups a manga and its related data (chapters, categories this manga is in, sync...).
     *
     * @param manga the manga to backup.
     * @return a JSON object containing all the data of the manga.
     */
    private fun backupManga(manga: Manga): JsonObject {
        // Entry for this manga
        val entry = JsonObject()

        // Backup manga fields
        entry.add(MANGA, gson.toJsonTree(manga))

        // Backup all the chapters
        val chapters = db.getChapters(manga).executeAsBlocking()
        if (!chapters.isEmpty()) {
            entry.add(CHAPTERS, gson.toJsonTree(chapters))
        }

        // Backup tracks
        val tracks = db.getTracks(manga).executeAsBlocking()
        if (!tracks.isEmpty()) {
            entry.add(TRACK, gson.toJsonTree(tracks))
        }

        // Backup categories for this manga
        val categoriesForManga = db.getCategoriesForManga(manga).executeAsBlocking()
        if (!categoriesForManga.isEmpty()) {
            val categoriesNames = ArrayList<String>()
            for (category in categoriesForManga) {
                categoriesNames.add(category.name)
            }
            entry.add(CATEGORIES, gson.toJsonTree(categoriesNames))
        }

        return entry
    }

    /**
     * Backups a category.
     *
     * @param category the category to backup.
     * @return a JSON object containing the data of the category.
     */
    private fun backupCategory(category: Category): JsonElement {
        return gson.toJsonTree(category)
    }

    /**
     * Restores a backup from a file.
     *
     * @param file the file containing the backup.
     * @throws IOException if there's any IO error.
     */
    @Throws(IOException::class)
    fun restoreFromFile(file: File) {
        JsonReader(FileReader(file)).use {
            val root = JsonParser().parse(it).asJsonObject
            restoreFromJson(root)
        }
    }

    /**
     * Restores a backup from an input stream.
     *
     * @param stream the stream containing the backup.
     * @throws IOException if there's any IO error.
     */
    @Throws(IOException::class)
    fun restoreFromStream(stream: InputStream) {
        JsonReader(InputStreamReader(stream)).use {
            val root = JsonParser().parse(it).asJsonObject
            restoreFromJson(root)
        }
    }

    /**
     * Restores a backup from a JSON object. Everything executes in a single transaction so that
     * nothing is modified if there's an error.
     *
     * @param root the root of the JSON.
     */
    fun restoreFromJson(root: JsonObject) {
        db.inTransaction {
            // Restore categories
            root.get(CATEGORIES)?.let {
                restoreCategories(it.asJsonArray)
            }

            // Restore mangas
            root.get(MANGAS)?.let {
                restoreMangas(it.asJsonArray)
            }
        }
    }

    /**
     * Restores the categories.
     *
     * @param jsonCategories the categories of the json.
     */
    private fun restoreCategories(jsonCategories: JsonArray) {
        // Get categories from file and from db
        val dbCategories = db.getCategories().executeAsBlocking()
        val backupCategories = gson.fromJson<List<CategoryImpl>>(jsonCategories)

        // Iterate over them
        for (category in backupCategories) {
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
                val result = db.insertCategory(category).executeAsBlocking()
                category.id = result.insertedId()?.toInt()
            }
        }
    }

    /**
     * Restores all the mangas and its related data.
     *
     * @param jsonMangas the mangas and its related data (chapters, sync, categories) from the json.
     */
    private fun restoreMangas(jsonMangas: JsonArray) {
        for (backupManga in jsonMangas) {
            // Map every entry to objects
            val element = backupManga.asJsonObject
            val manga = gson.fromJson(element.get(MANGA), MangaImpl::class.java)
            val chapters = gson.fromJson<List<ChapterImpl>>(element.get(CHAPTERS) ?: JsonArray())
            val tracks = gson.fromJson<List<TrackImpl>>(element.get(TRACK) ?: JsonArray())
            val categories = gson.fromJson<List<String>>(element.get(CATEGORIES) ?: JsonArray())

            // Restore everything related to this manga
            restoreManga(manga)
            restoreChaptersForManga(manga, chapters)
            restoreSyncForManga(manga, tracks)
            restoreCategoriesForManga(manga, categories)
        }
    }

    /**
     * Restores a manga.
     *
     * @param manga the manga to restore.
     */
    private fun restoreManga(manga: Manga) {
        // Try to find existing manga in db
        val dbManga = db.getManga(manga.url, manga.source).executeAsBlocking()
        if (dbManga == null) {
            // Let the db assign the id
            manga.id = null
            val result = db.insertManga(manga).executeAsBlocking()
            manga.id = result.insertedId()
        } else {
            // If it exists already, we copy only the values related to the source from the db
            // (they can be up to date). Local values (flags) are kept from the backup.
            manga.id = dbManga.id
            manga.copyFrom(dbManga)
            manga.favorite = true
            db.insertManga(manga).executeAsBlocking()
        }
    }

    /**
     * Restores the chapters of a manga.
     *
     * @param manga the manga whose chapters have to be restored.
     * @param chapters the chapters to restore.
     */
    private fun restoreChaptersForManga(manga: Manga, chapters: List<Chapter>) {
        // Fix foreign keys with the current manga id
        for (chapter in chapters) {
            chapter.manga_id = manga.id
        }

        val dbChapters = db.getChapters(manga).executeAsBlocking()
        val chaptersToUpdate = ArrayList<Chapter>()
        for (backupChapter in chapters) {
            // Try to find existing chapter in db
            val pos = dbChapters.indexOf(backupChapter)
            if (pos != -1) {
                // The chapter is already in the db, only update its fields
                val dbChapter = dbChapters[pos]
                // If one of them was read, the chapter will be marked as read
                dbChapter.read = backupChapter.read || dbChapter.read
                dbChapter.last_page_read = Math.max(backupChapter.last_page_read, dbChapter.last_page_read)
                chaptersToUpdate.add(dbChapter)
            } else {
                // Insert new chapter. Let the db assign the id
                backupChapter.id = null
                chaptersToUpdate.add(backupChapter)
            }
        }

        // Update database
        if (!chaptersToUpdate.isEmpty()) {
            db.insertChapters(chaptersToUpdate).executeAsBlocking()
        }
    }

    /**
     * Restores the categories a manga is in.
     *
     * @param manga the manga whose categories have to be restored.
     * @param categories the categories to restore.
     */
    private fun restoreCategoriesForManga(manga: Manga, categories: List<String>) {
        val dbCategories = db.getCategories().executeAsBlocking()
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
            db.deleteOldMangasCategories(mangaAsList).executeAsBlocking()
            db.insertMangasCategories(mangaCategoriesToUpdate).executeAsBlocking()
        }
    }

    /**
     * Restores the sync of a manga.
     *
     * @param manga the manga whose sync have to be restored.
     * @param tracks the track list to restore.
     */
    private fun restoreSyncForManga(manga: Manga, tracks: List<Track>) {
        // Fix foreign keys with the current manga id
        for (track in tracks) {
            track.manga_id = manga.id!!
        }

        val dbTracks = db.getTracks(manga).executeAsBlocking()
        val trackToUpdate = ArrayList<Track>()
        for (backupTrack in tracks) {
            // Try to find existing chapter in db
            val pos = dbTracks.indexOf(backupTrack)
            if (pos != -1) {
                // The sync is already in the db, only update its fields
                val dbSync = dbTracks[pos]
                // Mark the max chapter as read and nothing else
                dbSync.last_chapter_read = Math.max(backupTrack.last_chapter_read, dbSync.last_chapter_read)
                trackToUpdate.add(dbSync)
            } else {
                // Insert new sync. Let the db assign the id
                backupTrack.id = null
                trackToUpdate.add(backupTrack)
            }
        }

        // Update database
        if (!trackToUpdate.isEmpty()) {
            db.insertTracks(trackToUpdate).executeAsBlocking()
        }
    }

}
