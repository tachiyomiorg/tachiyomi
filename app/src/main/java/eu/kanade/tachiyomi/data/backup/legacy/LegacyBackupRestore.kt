package eu.kanade.tachiyomi.data.backup.legacy

import android.content.Context
import android.net.Uri
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.backup.BackupConst
import eu.kanade.tachiyomi.data.backup.BackupNotifier
import eu.kanade.tachiyomi.data.backup.legacy.models.Backup
import eu.kanade.tachiyomi.data.backup.legacy.models.Backup.MANGAS
import eu.kanade.tachiyomi.data.backup.legacy.models.DHistory
import eu.kanade.tachiyomi.data.backup.models.AbstractBackupRestore
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.ChapterImpl
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.database.models.TrackImpl
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.util.chapter.NoChaptersException
import rx.Observable
import java.util.Date

class LegacyBackupRestore(context: Context, notifier: BackupNotifier) : AbstractBackupRestore(context, notifier) {

    private lateinit var backupManager: LegacyBackupManager

    /**
     * Restores data from backup file.
     *
     * @param uri backup file to restore
     */
    override fun restoreBackup(uri: Uri): Boolean {
        val startTime = System.currentTimeMillis()

        val reader = JsonReader(context.contentResolver.openInputStream(uri)!!.bufferedReader())
        val json = JsonParser.parseReader(reader).asJsonObject

        // Get parser version
        val version = json.get(Backup.VERSION)?.asInt ?: 1

        // Initialize manager
        backupManager = LegacyBackupManager(context, version)

        val mangasJson = json.get(MANGAS).asJsonArray

        restoreAmount = mangasJson.size() + 3 // +1 for categories, +1 for saved searches, +1 for merged manga references
        restoreProgress = 0
        errors.clear()

        // Restore categories
        json.get(Backup.CATEGORIES)?.let { restoreCategories(it) }

        // Store source mapping for error messages
        sourceMapping = LegacyBackupRestoreValidator.getSourceMapping(json)

        // Restore individual manga
        mangasJson.forEach {
            if (job?.isActive != true) {
                return false
            }

            restoreManga(it.asJsonObject)
        }

        val endTime = System.currentTimeMillis()
        val time = endTime - startTime

        val logFile = writeErrorLog()

        notifier.showRestoreComplete(time, errors.size, logFile.parent, logFile.name)
        return true
    }

    private fun restoreCategories(categoriesJson: JsonElement) {
        db.inTransaction {
            backupManager.restoreCategories(categoriesJson.asJsonArray)
        }

        restoreProgress += 1
        showRestoreProgress(restoreProgress, restoreAmount, context.getString(R.string.categories))
    }

    private fun restoreManga(mangaJson: JsonObject) {
        val manga = backupManager.parser.fromJson<MangaImpl>(
            mangaJson.get(
                Backup.MANGA
            )
        )
        val chapters = backupManager.parser.fromJson<List<ChapterImpl>>(
            mangaJson.get(Backup.CHAPTERS)
                ?: JsonArray()
        )
        val categories = backupManager.parser.fromJson<List<String>>(
            mangaJson.get(Backup.CATEGORIES)
                ?: JsonArray()
        )
        val history = backupManager.parser.fromJson<List<DHistory>>(
            mangaJson.get(Backup.HISTORY)
                ?: JsonArray()
        )
        val tracks = backupManager.parser.fromJson<List<TrackImpl>>(
            mangaJson.get(Backup.TRACK)
                ?: JsonArray()
        )

        try {
            val source = backupManager.sourceManager.get(manga.source)
            if (source != null) {
                restoreMangaData(manga, source, chapters, categories, history, tracks)
            } else {
                val sourceName = sourceMapping[manga.source] ?: manga.source.toString()
                errors.add(Date() to "${manga.title} - ${context.getString(R.string.source_not_found_name, sourceName)}")
            }
        } catch (e: Exception) {
            errors.add(Date() to "${manga.title} - ${e.message}")
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
    private fun restoreMangaData(
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
     * [Observable] that fetches manga information
     *
     * @param manga manga that needs updating
     * @param chapters chapters of manga that needs updating
     * @param categories categories that need updating
     */
    private fun restoreMangaFetch(
        source: Source,
        manga: Manga,
        chapters: List<Chapter>,
        categories: List<String>,
        history: List<DHistory>,
        tracks: List<Track>
    ) {
        backupManager.restoreMangaFetchObservable(source, manga)
            .onErrorReturn {
                errors.add(Date() to "${manga.title} - ${it.message}")
                manga
            }
            .filter { it.id != null }
            .flatMap {
                chapterFetchObservable(source, it, chapters)
                    // Convert to the manga that contains new chapters.
                    .map { manga }
            }
            .doOnNext {
                restoreExtraForManga(it, categories, history, tracks)
            }
            .flatMap {
                trackingFetchObservable(it, tracks)
            }
            .subscribe()
    }

    private fun restoreMangaNoFetch(
        source: Source,
        backupManga: Manga,
        chapters: List<Chapter>,
        categories: List<String>,
        history: List<DHistory>,
        tracks: List<Track>
    ) {
        Observable.just(backupManga)
            .flatMap { manga ->
                if (!backupManager.restoreChaptersForManga(manga, chapters)) {
                    chapterFetchObservable(source, manga, chapters)
                        .map { manga }
                } else {
                    Observable.just(manga)
                }
            }
            .doOnNext {
                restoreExtraForManga(it, categories, history, tracks)
            }
            .flatMap { manga ->
                trackingFetchObservable(manga, tracks)
            }
            .subscribe()
    }

    private fun restoreExtraForManga(manga: Manga, categories: List<String>, history: List<DHistory>, tracks: List<Track>) {
        // Restore categories
        backupManager.restoreCategoriesForManga(manga, categories)

        // Restore history
        backupManager.restoreHistoryForManga(history)

        // Restore tracking
        backupManager.restoreTrackForManga(manga, tracks)
    }

    /**
     * [Observable] that fetches chapter information
     *
     * @param source source of manga
     * @param manga manga that needs updating
     * @return [Observable] that contains manga
     */
    private fun chapterFetchObservable(source: Source, manga: Manga, chapters: List<Chapter>): Observable<Pair<List<Chapter>, List<Chapter>>> {
        return backupManager.restoreChapterFetchObservable(source, manga, chapters)
            // If there's any error, return empty update and continue.
            .onErrorReturn {
                val errorMessage = if (it is NoChaptersException) {
                    context.getString(R.string.no_chapters_error)
                } else {
                    it.message
                }
                errors.add(Date() to "${manga.title} - $errorMessage")
                Pair(emptyList(), emptyList())
            }
    }

    /**
     * [Observable] that refreshes tracking information
     * @param manga manga that needs updating.
     * @param tracks list containing tracks from restore file.
     * @return [Observable] that contains updated track item
     */
    private fun trackingFetchObservable(manga: Manga, tracks: List<Track>): Observable<Track> {
        return Observable.from(tracks)
            .flatMap { track ->
                val service = trackManager.getService(track.sync_id)
                if (service != null && service.isLogged) {
                    service.refresh(track)
                        .doOnNext { db.insertTrack(it).executeAsBlocking() }
                        .onErrorReturn {
                            errors.add(Date() to "${manga.title} - ${it.message}")
                            track
                        }
                } else {
                    errors.add(Date() to "${manga.title} - ${context.getString(R.string.tracker_not_logged_in, service?.name)}")
                    Observable.empty()
                }
            }
    }

    /**
     * Called to update dialog in [BackupConst]
     *
     * @param progress restore progress
     * @param amount total restoreAmount of manga
     * @param title title of restored manga
     */
    private fun showRestoreProgress(
        progress: Int,
        amount: Int,
        title: String
    ) {
        notifier.showRestoreProgress(title, progress, amount)
    }
}
