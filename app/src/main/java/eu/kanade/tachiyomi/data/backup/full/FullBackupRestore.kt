package eu.kanade.tachiyomi.data.backup.full

import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.backup.BackupNotifier
import eu.kanade.tachiyomi.data.backup.full.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.full.models.BackupHistory
import eu.kanade.tachiyomi.data.backup.full.models.BackupManga
import eu.kanade.tachiyomi.data.backup.full.models.BackupSerializer
import eu.kanade.tachiyomi.data.backup.models.AbstractBackupRestore
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.util.chapter.NoChaptersException
import kotlinx.serialization.ExperimentalSerializationApi
import okio.buffer
import okio.gzip
import okio.source
import rx.Observable
import java.util.Date

@OptIn(ExperimentalSerializationApi::class)
class FullBackupRestore(context: Context, notifier: BackupNotifier, private val online: Boolean) : AbstractBackupRestore(context, notifier) {
    private lateinit var fullBackupManager: FullBackupManager

    /**
     * Restores data from backup file.
     *
     * @param uri backup file to restore
     */
    override fun restoreBackup(uri: Uri): Boolean {
        val startTime = System.currentTimeMillis()

        // Initialize manager
        fullBackupManager = FullBackupManager(context)

        val backupString = context.contentResolver.openInputStream(uri)!!.source().gzip().buffer().use { it.readByteArray() }
        val backup = fullBackupManager.parser.decodeFromByteArray(BackupSerializer, backupString)

        restoreAmount = backup.backupManga.size + 1 // +1 for categories
        restoreProgress = 0
        errors.clear()

        // Restore categories
        if (backup.backupCategories.isNotEmpty()) {
            restoreCategories(backup.backupCategories)
        }

        // Store source mapping for error messages
        sourceMapping = backup.backupSources.map { it.sourceId to it.name }.toMap()

        // Restore individual manga, sort by merged source so that merged source manga go last and merged references get the proper ids
        backup.backupManga.forEach {
            if (job?.isActive != true) {
                return false
            }

            restoreManga(it, backup.backupCategories, online)
        }

        val endTime = System.currentTimeMillis()
        val time = endTime - startTime

        val logFile = writeErrorLog()

        notifier.showRestoreComplete(time, errors.size, logFile.parent, logFile.name)
        return true
    }

    private fun restoreCategories(backupCategories: List<BackupCategory>) {
        db.inTransaction {
            fullBackupManager.restoreCategories(backupCategories)
        }

        restoreProgress += 1
        showRestoreProgress(restoreProgress, restoreAmount, context.getString(R.string.categories))
    }

    private fun restoreManga(backupManga: BackupManga, backupCategories: List<BackupCategory>, online: Boolean) {
        val manga = backupManga.getMangaImpl()
        val chapters = backupManga.getChaptersImpl()
        val categories = backupManga.categories
        val history = backupManga.history
        val tracks = backupManga.getTrackingImpl()

        try {
            val source = fullBackupManager.sourceManager.get(manga.source)
            if (source != null || !online) {
                restoreMangaData(manga, source, chapters, categories, history, tracks, backupCategories, online)
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
        source: Source?,
        chapters: List<Chapter>,
        categories: List<Int>,
        history: List<BackupHistory>,
        tracks: List<Track>,
        backupCategories: List<BackupCategory>,
        online: Boolean
    ) {
        val dbManga = fullBackupManager.getMangaFromDatabase(manga)

        db.inTransaction {
            if (dbManga == null) {
                // Manga not in database
                restoreMangaFetch(source, manga, chapters, categories, history, tracks, backupCategories, online)
            } else { // Manga in database
                // Copy information from manga already in database
                fullBackupManager.restoreMangaNoFetch(manga, dbManga)
                // Fetch rest of manga information
                restoreMangaNoFetch(source, manga, chapters, categories, history, tracks, backupCategories, online)
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
        source: Source?,
        manga: Manga,
        chapters: List<Chapter>,
        categories: List<Int>,
        history: List<BackupHistory>,
        tracks: List<Track>,
        backupCategories: List<BackupCategory>,
        online: Boolean
    ) {
        fullBackupManager.restoreMangaFetchObservable(source, manga, online)
            .doOnError {
                errors.add(Date() to "${manga.title} - ${it.message}")
            }
            .filter { it.id != null }
            .flatMap {
                if (online && source != null) {
                    chapterFetchObservable(source, it, chapters)
                        // Convert to the manga that contains new chapters.
                        .map { manga }
                } else {
                    fullBackupManager.restoreChaptersForMangaOffline(it, chapters)
                    Observable.just(manga)
                }
            }
            .doOnNext {
                restoreExtraForManga(it, categories, history, tracks, backupCategories)
            }
            .flatMap {
                trackingFetchObservable(it, tracks)
            }
            .subscribe()
    }

    private fun restoreMangaNoFetch(
        source: Source?,
        backupManga: Manga,
        chapters: List<Chapter>,
        categories: List<Int>,
        history: List<BackupHistory>,
        tracks: List<Track>,
        backupCategories: List<BackupCategory>,
        online: Boolean
    ) {
        Observable.just(backupManga)
            .flatMap { manga ->
                if (online && source != null) {
                    if (!fullBackupManager.restoreChaptersForManga(manga, chapters)) {
                        chapterFetchObservable(source, manga, chapters)
                            .map { manga }
                    } else {
                        Observable.just(manga)
                    }
                } else {
                    fullBackupManager.restoreChaptersForMangaOffline(manga, chapters)
                    Observable.just(manga)
                }
            }
            .doOnNext {
                restoreExtraForManga(it, categories, history, tracks, backupCategories)
            }
            .flatMap { manga ->
                trackingFetchObservable(manga, tracks)
            }
            .subscribe()
    }

    private fun restoreExtraForManga(manga: Manga, categories: List<Int>, history: List<BackupHistory>, tracks: List<Track>, backupCategories: List<BackupCategory>) {
        // Restore categories
        fullBackupManager.restoreCategoriesForManga(manga, categories, backupCategories)

        // Restore history
        fullBackupManager.restoreHistoryForManga(history)

        // Restore tracking
        fullBackupManager.restoreTrackForManga(manga, tracks)
    }

    /**
     * [Observable] that fetches chapter information
     *
     * @param source source of manga
     * @param manga manga that needs updating
     * @return [Observable] that contains manga
     */
    private fun chapterFetchObservable(source: Source, manga: Manga, chapters: List<Chapter>): Observable<Pair<List<Chapter>, List<Chapter>>> {
        return fullBackupManager.restoreChapterFetchObservable(source, manga, chapters)
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
