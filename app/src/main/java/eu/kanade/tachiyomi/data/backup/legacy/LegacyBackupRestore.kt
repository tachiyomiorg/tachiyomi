package eu.kanade.tachiyomi.data.backup.legacy

import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.backup.AbstractBackupRestore
import eu.kanade.tachiyomi.data.backup.BackupNotifier
import eu.kanade.tachiyomi.data.backup.legacy.models.Backup
import eu.kanade.tachiyomi.data.backup.legacy.models.DHistory
import eu.kanade.tachiyomi.data.backup.legacy.models.MangaObject
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.source.Source
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import okio.buffer
import okio.source
import java.util.Date

class LegacyBackupRestore(context: Context, notifier: BackupNotifier) : AbstractBackupRestore<LegacyBackupManager>(context, notifier) {

    override suspend fun performRestore(uri: Uri): Boolean {
        // Read the json and create a Json Object,
        // cannot use the backupManager json deserializer one because its not initialized yet
        val backupObject = Json.decodeFromString<JsonObject>(
            context.contentResolver.openInputStream(uri)!!.source().buffer().use { it.readUtf8() }
        )

        // Get parser version
        val version = backupObject["version"]?.jsonPrimitive?.intOrNull ?: 1

        // Initialize manager
        backupManager = LegacyBackupManager(context, version)

        // Decode the json object to a Backup object
        val backup = backupManager.parser.decodeFromJsonElement<Backup>(backupObject)

        restoreAmount = backup.mangas.size + 1 // +1 for categories

        // Restore categories
        backup.categories?.let { restoreCategories(it) }

        // Store source mapping for error messages
        sourceMapping = LegacyBackupRestoreValidator.getSourceMapping(backup.extensions ?: emptyList())

        // Restore individual manga
        backup.mangas.forEach {
            if (job?.isActive != true) {
                return false
            }

            restoreManga(it)
        }

        return true
    }

    private fun restoreCategories(categoriesJson: List<Category>) {
        db.inTransaction {
            backupManager.restoreCategories(categoriesJson)
        }

        restoreProgress += 1
        showRestoreProgress(restoreProgress, restoreAmount, context.getString(R.string.categories))
    }

    private suspend fun restoreManga(mangaJson: MangaObject) {
        val manga = mangaJson.manga
        val chapters = mangaJson.chapters ?: emptyList()
        val categories = mangaJson.categories ?: emptyList()
        val history = mangaJson.history ?: emptyList()
        val tracks = mangaJson.track ?: emptyList()

        val source = backupManager.sourceManager.get(manga.source)
        val sourceName = sourceMapping[manga.source] ?: manga.source.toString()

        try {
            if (source != null) {
                restoreMangaData(manga, source, chapters, categories, history, tracks)
            } else {
                errors.add(Date() to "${manga.title} [$sourceName]: ${context.getString(R.string.source_not_found_name, sourceName)}")
            }
        } catch (e: Exception) {
            errors.add(Date() to "${manga.title} [$sourceName]: ${e.message}")
        }

        restoreProgress += 1
        showRestoreProgress(restoreProgress, restoreAmount, manga.title)
    }

    /**
     * Returns a manga restore observable
     *
     * @param manga manga data from json
     * @param source source to get manga data from
     * @param chapters chapters data from json
     * @param categories categories data from json
     * @param history history data from json
     * @param tracks tracking data from json
     */
    private suspend fun restoreMangaData(
        manga: Manga,
        source: Source,
        chapters: List<Chapter>,
        categories: List<String>,
        history: List<DHistory>,
        tracks: List<Track>
    ) {
        val dbManga = backupManager.getMangaFromDatabase(manga)

        db.inTransaction {
            if (dbManga == null) {
                // Manga not in database
                restoreMangaFetch(source, manga, chapters, categories, history, tracks)
            } else { // Manga in database
                // Copy information from manga already in database
                backupManager.restoreMangaNoFetch(manga, dbManga)
                // Fetch rest of manga information
                restoreMangaNoFetch(source, manga, chapters, categories, history, tracks)
            }
        }
    }

    /**
     * Fetches manga information.
     *
     * @param manga manga that needs updating
     * @param chapters chapters of manga that needs updating
     * @param categories categories that need updating
     */
    private suspend fun restoreMangaFetch(
        source: Source,
        manga: Manga,
        chapters: List<Chapter>,
        categories: List<String>,
        history: List<DHistory>,
        tracks: List<Track>
    ) {
        try {
            val fetchedManga = backupManager.fetchManga(source, manga)
            fetchedManga.id ?: return

            updateChapters(source, fetchedManga, chapters)

            restoreExtraForManga(fetchedManga, categories, history, tracks)

            updateTracking(fetchedManga, tracks)
        } catch (e: Exception) {
            errors.add(Date() to "${manga.title} - ${e.message}")
        }
    }

    private suspend fun restoreMangaNoFetch(
        source: Source,
        backupManga: Manga,
        chapters: List<Chapter>,
        categories: List<String>,
        history: List<DHistory>,
        tracks: List<Track>
    ) {
        if (!backupManager.restoreChaptersForManga(backupManga, chapters)) {
            updateChapters(source, backupManga, chapters)
        }

        restoreExtraForManga(backupManga, categories, history, tracks)

        updateTracking(backupManga, tracks)
    }

    private fun restoreExtraForManga(manga: Manga, categories: List<String>, history: List<DHistory>, tracks: List<Track>) {
        // Restore categories
        backupManager.restoreCategoriesForManga(manga, categories)

        // Restore history
        backupManager.restoreHistoryForManga(history)

        // Restore tracking
        backupManager.restoreTrackForManga(manga, tracks)
    }
}
