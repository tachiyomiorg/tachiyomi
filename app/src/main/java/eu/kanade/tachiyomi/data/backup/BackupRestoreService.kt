package eu.kanade.tachiyomi.data.backup

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.os.PowerManager
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.JsonArray
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
import eu.kanade.tachiyomi.data.database.models.*
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.util.chop
import eu.kanade.tachiyomi.util.isServiceRunning
import eu.kanade.tachiyomi.util.sendLocalBroadcast
import rx.Observable
import rx.Subscription
import rx.schedulers.Schedulers
import timber.log.Timber
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Restores backup from json file
 */
class BackupRestoreService : Service() {

    companion object {

        /**
         * Returns the status of the service.
         *
         * @param context the application context.
         * @return true if the service is running, false otherwise.
         */
        private fun isRunning(context: Context): Boolean =
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
                context.startService(intent)
            }
        }

        /**
         * Stops the service.
         *
         * @param context the application context.
         */
        fun stop(context: Context) {
            context.stopService(Intent(context, BackupRestoreService::class.java))
        }
    }

    /**
     * Wake lock that will be held until the service is destroyed.
     */
    private lateinit var wakeLock: PowerManager.WakeLock

    /**
     * Subscription where the update is done.
     */
    private var subscription: Subscription? = null

    /**
     * The progress of a backup restore
     */
    private var restoreProgress = 0

    /**
     * Amount of manga in Json file (needed for restore)
     */
    private var restoreAmount = 0

    /**
     * List containing errors
     */
    private val errors = mutableListOf<Pair<Date, String>>()

    /**
     * Backup manager
     */
    private lateinit var backupManager: BackupManager

    /**
     * Database
     */
    private val db: DatabaseHelper by injectLazy()

    /**
     * Tracking manager
     */
    internal val trackManager: TrackManager by injectLazy()


    private lateinit var executor: ExecutorService

    /**
     * Method called when the service is created. It injects dependencies and acquire the wake lock.
     */
    override fun onCreate() {
        super.onCreate()
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, "BackupRestoreService:WakeLock")
        wakeLock.acquire()
        executor = Executors.newSingleThreadExecutor()
    }

    /**
     * Method called when the service is destroyed. It destroys the running subscription and
     * releases the wake lock.
     */
    override fun onDestroy() {
        subscription?.unsubscribe()
        executor.shutdown() // must be called after unsubscribe
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        super.onDestroy()
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
        if (intent == null) return Service.START_NOT_STICKY

        val uri = intent.getParcelableExtra<Uri>(BackupConst.EXTRA_URI)

        // Unsubscribe from any previous subscription if needed.
        subscription?.unsubscribe()

        subscription = Observable.using(
                { db.lowLevel().beginTransaction() },
                { getRestoreObservable(uri).doOnNext { db.lowLevel().setTransactionSuccessful() } },
                { executor.execute { db.lowLevel().endTransaction() } })
                .doAfterTerminate { stopSelf(startId) }
                .subscribeOn(Schedulers.from(executor))
                .subscribe()

        return Service.START_NOT_STICKY
    }

    /**
     * Returns an [Observable] containing restore process.
     *
     * @param uri restore file
     * @return [Observable<Manga>]
     */
    private fun getRestoreObservable(uri: Uri): Observable<List<Manga>> {
        val startTime = System.currentTimeMillis()

        return Observable.just(Unit)
                .map {
                    val reader = JsonReader(contentResolver.openInputStream(uri).bufferedReader())
                    val json = JsonParser().parse(reader).asJsonObject

                    // Get parser version
                    val version = json.get(VERSION)?.asInt ?: 1

                    // Initialize manager
                    backupManager = BackupManager(this, version)

                    val mangasJson = json.get(MANGAS).asJsonArray

                    restoreAmount = mangasJson.size() + 1 // +1 for categories
                    restoreProgress = 0
                    errors.clear()

                    // Restore categories
                    json.get(CATEGORIES)?.let {
                        backupManager.restoreCategories(it.asJsonArray)
                        restoreProgress += 1
                        showRestoreProgress(restoreProgress, restoreAmount, "Categories added", errors.size)
                    }

                    mangasJson
                }
                .flatMap { Observable.from(it) }
                .concatMap {
                    val obj = it.asJsonObject
                    val manga = backupManager.parser.fromJson<MangaImpl>(obj.get(MANGA))
                    val chapters = backupManager.parser.fromJson<List<ChapterImpl>>(obj.get(CHAPTERS) ?: JsonArray())
                    val categories = backupManager.parser.fromJson<List<String>>(obj.get(CATEGORIES) ?: JsonArray())
                    val history = backupManager.parser.fromJson<List<DHistory>>(obj.get(HISTORY) ?: JsonArray())
                    val tracks = backupManager.parser.fromJson<List<TrackImpl>>(obj.get(TRACK) ?: JsonArray())

                    val observable = getMangaRestoreObservable(manga, chapters, categories, history, tracks)
                    if (observable != null) {
                        observable
                    } else {
                        errors.add(Date() to "${manga.title} - ${getString(R.string.source_not_found)}")
                        restoreProgress += 1
                        val content = getString(R.string.dialog_restoring_source_not_found, manga.title.chop(15))
                        showRestoreProgress(restoreProgress, restoreAmount, manga.title, errors.size, content)
                        Observable.just(manga)
                    }
                }
                .toList()
                .doOnNext {
                    val endTime = System.currentTimeMillis()
                    val time = endTime - startTime
                    val logFile = writeErrorLog()
                    val completeIntent = Intent(BackupConst.INTENT_FILTER).apply {
                        putExtra(BackupConst.EXTRA_TIME, time)
                        putExtra(BackupConst.EXTRA_ERRORS, errors.size)
                        putExtra(BackupConst.EXTRA_ERROR_FILE_PATH, logFile.parent)
                        putExtra(BackupConst.EXTRA_ERROR_FILE, logFile.name)
                        putExtra(BackupConst.ACTION, BackupConst.ACTION_RESTORE_COMPLETED_DIALOG)
                    }
                    sendLocalBroadcast(completeIntent)

                }
                .doOnError { error ->
                    Timber.e(error)
                    writeErrorLog()
                    val errorIntent = Intent(BackupConst.INTENT_FILTER).apply {
                        putExtra(BackupConst.ACTION, BackupConst.ACTION_ERROR_RESTORE_DIALOG)
                        putExtra(BackupConst.EXTRA_ERROR_MESSAGE, error.message)
                    }
                    sendLocalBroadcast(errorIntent)
                }
                .onErrorReturn { emptyList() }
    }

    /**
     * Write errors to error log
     */
    private fun writeErrorLog(): File {
        try {
            if (errors.isNotEmpty()) {
                val destFile = File(externalCacheDir, "tachiyomi_restore.log")
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

    /**
     * Returns a manga restore observable
     *
     * @param manga manga data from json
     * @param chapters chapters data from json
     * @param categories categories data from json
     * @param history history data from json
     * @param tracks tracking data from json
     * @return [Observable] containing manga restore information
     */
    private fun getMangaRestoreObservable(manga: Manga, chapters: List<Chapter>,
                                          categories: List<String>, history: List<DHistory>,
                                          tracks: List<Track>): Observable<Manga>? {
        // Get source
        val source = backupManager.sourceManager.getOrStub(manga.source)
        val dbManga = backupManager.getMangaFromDatabase(manga)

        return if (dbManga == null) {
            // Manga not in database
            mangaFetchObservable(source, manga, chapters, categories, history, tracks)
        } else { // Manga in database
            // Copy information from manga already in database
            backupManager.restoreMangaNoFetch(manga, dbManga)
            // Fetch rest of manga information
            mangaNoFetchObservable(source, manga, chapters, categories, history, tracks)
        }
    }

    /**
     * [Observable] that fetches manga information
     *
     * @param manga manga that needs updating
     * @param chapters chapters of manga that needs updating
     * @param categories categories that need updating
     */
    private fun mangaFetchObservable(source: Source, manga: Manga, chapters: List<Chapter>,
                                     categories: List<String>, history: List<DHistory>,
                                     tracks: List<Track>): Observable<Manga> {
        return backupManager.restoreMangaFetchObservable(source, manga)
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
                            // Convert to the manga that contains new chapters.
                            .map { manga }
                }
                .doOnCompleted {
                    restoreProgress += 1
                    showRestoreProgress(restoreProgress, restoreAmount, manga.title, errors.size)
                }
    }

    private fun mangaNoFetchObservable(source: Source, backupManga: Manga, chapters: List<Chapter>,
                                       categories: List<String>, history: List<DHistory>,
                                       tracks: List<Track>): Observable<Manga> {

        return Observable.just(backupManga)
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
                            // Convert to the manga that contains new chapters.
                            .map { manga }
                }
                .doOnCompleted {
                    restoreProgress += 1
                    showRestoreProgress(restoreProgress, restoreAmount, backupManga.title, errors.size)
                }
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
                    errors.add(Date() to "${manga.title} - ${it.message}")
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
                .concatMap { track ->
                    val service = trackManager.getService(track.sync_id)
                    if (service != null && service.isLogged) {
                        service.refresh(track)
                                .doOnNext { db.insertTrack(it).executeAsBlocking() }
                                .onErrorReturn {
                                    errors.add(Date() to "${manga.title} - ${it.message}")
                                    track
                                }
                    } else {
                        errors.add(Date() to "${manga.title} - ${service?.name} not logged in")
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
    private fun showRestoreProgress(progress: Int, amount: Int, title: String, errors: Int,
                                    content: String = getString(R.string.dialog_restoring_backup, title.chop(15))) {
        val intent = Intent(BackupConst.INTENT_FILTER).apply {
            putExtra(BackupConst.EXTRA_PROGRESS, progress)
            putExtra(BackupConst.EXTRA_AMOUNT, amount)
            putExtra(BackupConst.EXTRA_CONTENT, content)
            putExtra(BackupConst.EXTRA_ERRORS, errors)
            putExtra(BackupConst.ACTION, BackupConst.ACTION_SET_PROGRESS_DIALOG)
        }
        sendLocalBroadcast(intent)
    }

}
