package eu.kanade.tachiyomi.data.backup

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.backup.models.Backup.CATEGORIES
import eu.kanade.tachiyomi.data.backup.models.Backup.CHAPTERS
import eu.kanade.tachiyomi.data.backup.models.Backup.HISTORY
import eu.kanade.tachiyomi.data.backup.models.Backup.MANGA
import eu.kanade.tachiyomi.data.backup.models.Backup.MANGAS
import eu.kanade.tachiyomi.data.backup.models.Backup.TRACK
import eu.kanade.tachiyomi.data.backup.models.Backup.VERSION
import eu.kanade.tachiyomi.data.backup.models.DHistory
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.ChapterImpl
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.database.models.TrackImpl
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.util.chapter.NoChaptersException
import eu.kanade.tachiyomi.util.system.acquireWakeLock
import eu.kanade.tachiyomi.util.system.isServiceRunning
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import rx.Observable
import timber.log.Timber
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Restores backup from a JSON file.
 */
class BackupRestoreService : Service() {

    companion object {

        /**
         * Returns the status of the service.
         *
         * @param context the application context.
         * @return true if the service is running, false otherwise.
         */
        fun isRunning(context: Context): Boolean =
            context.isServiceRunning(BackupRestoreService::class.java)

        /**
         * Starts a service to restore a backup from Json
         *
         * @param context context of application
         * @param uri path of Uri
         */
        fun start(context: Context, uri: Uri) {
            if (!isRunning(context)) {
                val intent = Intent(context, BackupRestoreService::class.java).apply {
                    putExtra(BackupConst.EXTRA_URI, uri)
                }
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    context.startService(intent)
                } else {
                    context.startForegroundService(intent)
                }
            }
        }

        /**
         * Stops the service.
         *
         * @param context the application context.
         */
        fun stop(context: Context) {
            context.stopService(Intent(context, BackupRestoreService::class.java))

            BackupNotifier(context).showRestoreError(context.getString(R.string.restoring_backup_canceled))
        }
    }

    /**
     * Wake lock that will be held until the service is destroyed.
     */
    private lateinit var wakeLock: PowerManager.WakeLock

    private var job: Job? = null

    /**
     * The progress of a backup restore
     */
    private var restoreProgress = 0

    /**
     * Amount of manga in Json file (needed for restore)
     */
    private var restoreAmount = 0

    /**
     * Mapping of source ID to source name from backup data
     */
    private var sourceMapping: Map<Long, String> = emptyMap()

    /**
     * List containing errors
     */
    private val errors = mutableListOf<Pair<Date, String>>()

    private lateinit var backupManager: BackupManager
    private lateinit var notifier: BackupNotifier

    private val db: DatabaseHelper by injectLazy()

    private val trackManager: TrackManager by injectLazy()

    override fun onCreate() {
        super.onCreate()

        notifier = BackupNotifier(this)
        wakeLock = acquireWakeLock(javaClass.name)

        startForeground(Notifications.ID_RESTORE_PROGRESS, notifier.showRestoreProgress().build())
    }

    override fun stopService(name: Intent?): Boolean {
        destroyJob()
        return super.stopService(name)
    }

    override fun onDestroy() {
        destroyJob()
        super.onDestroy()
    }

    private fun destroyJob() {
        job?.cancel()
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    /**
     * This method needs to be implemented, but it's not used/needed.
     */
    override fun onBind(intent: Intent): IBinder? = null

    /**
     * Method called when the service receives an intent.
     *
     * @param intent the start intent from.
     * @param flags the flags of the command.
     * @param startId the start id of this command.
     * @return the start value of the command.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val uri = intent?.getParcelableExtra<Uri>(BackupConst.EXTRA_URI) ?: return START_NOT_STICKY

        // Cancel any previous job if needed.
        job?.cancel()
        val handler = CoroutineExceptionHandler { _, exception ->
            Timber.e(exception)
            writeErrorLog()

            notifier.showRestoreError(exception.message)

            stopSelf(startId)
        }
        job = GlobalScope.launch(handler) {
            if (!restoreBackup(uri)) {
                notifier.showRestoreError(getString(R.string.restoring_backup_canceled))
            }
        }
        job?.invokeOnCompletion {
            stopSelf(startId)
        }

        return START_NOT_STICKY
    }

    /**
     * Restores data from backup file.
     *
     * @param uri backup file to restore
     */
    private fun restoreBackup(uri: Uri): Boolean {
        val startTime = System.currentTimeMillis()

        val reader = JsonReader(contentResolver.openInputStream(uri)!!.bufferedReader())
        val json = JsonParser.parseReader(reader).asJsonObject

        // Get parser version
        val version = json.get(VERSION)?.asInt ?: 1

        // Initialize manager
        backupManager = BackupManager(this, version)

        val mangasJson = json.get(MANGAS).asJsonArray

        restoreAmount = mangasJson.size() + 1 // +1 for categories
        restoreProgress = 0
        errors.clear()

        // Restore categories
        json.get(CATEGORIES)?.let { restoreCategories(it) }

        // Store source mapping for error messages
        sourceMapping = BackupRestoreValidator.getSourceMapping(json)

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
        showRestoreProgress(restoreProgress, restoreAmount, getString(R.string.categories))
    }

    private fun restoreManga(mangaJson: JsonObject) {
        val manga = backupManager.parser.fromJson<MangaImpl>(mangaJson.get(MANGA))
        val chapters = backupManager.parser.fromJson<List<ChapterImpl>>(
            mangaJson.get(CHAPTERS)
                ?: JsonArray()
        )
        val categories = backupManager.parser.fromJson<List<String>>(
            mangaJson.get(CATEGORIES)
                ?: JsonArray()
        )
        val history = backupManager.parser.fromJson<List<DHistory>>(
            mangaJson.get(HISTORY)
                ?: JsonArray()
        )
        val tracks = backupManager.parser.fromJson<List<TrackImpl>>(
            mangaJson.get(TRACK)
                ?: JsonArray()
        )

        try {
            val source = backupManager.sourceManager.get(manga.source)
            if (source != null) {
                restoreMangaData(manga, source, chapters, categories, history, tracks)
            } else {
                val sourceName = sourceMapping[manga.source] ?: manga.source.toString()
                errors.add(Date() to "${manga.title} - ${getString(R.string.source_not_found_name, sourceName)}")
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
                    getString(R.string.no_chapters_error)
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
                    errors.add(Date() to "${manga.title} - ${getString(R.string.tracker_not_logged_in, service?.name)}")
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

    /**
     * Write errors to error log
     */
    private fun writeErrorLog(): File {
        try {
            if (errors.isNotEmpty()) {
                val destFile = File(externalCacheDir, "tachiyomi_restore.txt")
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

                destFile.bufferedWriter().use { out ->
                    errors.forEach { (date, message) ->
                        out.write("[${sdf.format(date)}] $message\n")
                    }
                }
                return destFile
            }
        } catch (e: Exception) {
            // Empty
        }
        return File("")
    }
}
