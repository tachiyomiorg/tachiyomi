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
import com.hippo.unifile.UniFile
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
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.setting.SettingsBackupFragment
import eu.kanade.tachiyomi.util.AndroidComponentUtil
import eu.kanade.tachiyomi.util.chop
import eu.kanade.tachiyomi.util.sendLocalBroadcast
import rx.Observable
import rx.Subscription
import rx.schedulers.Schedulers
import timber.log.Timber
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import eu.kanade.tachiyomi.BuildConfig.APPLICATION_ID as ID

/**
 * Restores backup from json file
 */
class BackupRestoreService : Service() {

    companion object {
        // Name of service
        private const val NAME = "BackupRestoreService"

        // Uri as string
        private const val EXTRA_URI = "$ID.$NAME.EXTRA_URI"

        /**
         * Returns the status of the service.
         *
         * @param context the application context.
         * @return true if the service is running, false otherwise.
         */
        fun isRunning(context: Context): Boolean {
            return AndroidComponentUtil.isServiceRunning(context, BackupRestoreService::class.java)
        }

        /**
         * Starts a service to restore a backup from Json
         *
         * @param context context of application
         * @param uri path of Uri
         */
        fun start(context: Context, uri: String) {
            if (!isRunning(context)) {
                val intent = Intent(context, BackupRestoreService::class.java).apply {
                    putExtra(EXTRA_URI, uri)
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
     * Method called when the service is created. It injects dependencies and acquire the wake lock.
     */
    override fun onCreate() {
        super.onCreate()
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, "BackupRestoreService:WakeLock")
        wakeLock.acquire()
    }

    /**
     * Method called when the service is destroyed. It destroys the running subscription and
     * releases the wake lock.
     */
    override fun onDestroy() {
        subscription?.unsubscribe()
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        super.onDestroy()
    }

    /**
     * This method needs to be implemented, but it's not used/needed.
     */
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

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

        // Unsubscribe from any previous subscription if needed.
        subscription?.unsubscribe()

        val startTime = System.currentTimeMillis()
        subscription = Observable.defer {
            // Get URI
            val uri = Uri.parse(intent.getStringExtra(EXTRA_URI))
            // Get file from Uri
            val file = UniFile.fromUri(this, uri)

            // Clear errors
            errors.clear()

            // Reset progress
            restoreProgress = 0

            db.lowLevel().beginTransaction()
            getRestoreObservable(file)
        }
        .subscribeOn(Schedulers.io())
        .subscribe({
        }, { error ->
            db.lowLevel().endTransaction()
            Timber.e(error)
            writeErrorLog()
            val errorIntent = Intent(SettingsBackupFragment.INTENT_FILTER).apply {
                putExtra(SettingsBackupFragment.ACTION, SettingsBackupFragment.ACTION_ERROR_RESTORE_DIALOG)
                putExtra(SettingsBackupFragment.EXTRA_ERROR_MESSAGE, error.message)
            }
            sendLocalBroadcast(errorIntent)
            stopSelf(startId)
        }, {
            db.lowLevel().setTransactionSuccessful()
            db.lowLevel().endTransaction()
            val endTime = System.currentTimeMillis()
            val time = endTime - startTime
            val file = writeErrorLog()
            val completeIntent = Intent(SettingsBackupFragment.INTENT_FILTER).apply {
                putExtra(SettingsBackupFragment.EXTRA_TIME, time)
                putExtra(SettingsBackupFragment.EXTRA_ERRORS, errors.size)
                putExtra(SettingsBackupFragment.EXTRA_ERROR_FILE_PATH, file.parent)
                putExtra(SettingsBackupFragment.EXTRA_ERROR_FILE, file.name)
                putExtra(SettingsBackupFragment.ACTION, SettingsBackupFragment.ACTION_RESTORE_COMPLETED_DIALOG)
            }
            sendLocalBroadcast(completeIntent)
            stopSelf(startId)
        })
        return Service.START_NOT_STICKY
    }

    /**
     * Returns an [Observable] containing restore process.
     *
     * @param file restore file
     * @return [Observable<Manga>]
     */
    private fun getRestoreObservable(file: UniFile): Observable<Manga> {
        val reader = JsonReader(file.openInputStream().bufferedReader())
        val json = JsonParser().parse(reader).asJsonObject

        // Get parser version
        val version = json.get(VERSION)?.asInt ?: 1

        // Initialize manager
        backupManager = BackupManager(this, version)

        val mangasJson = json.get(MANGAS).asJsonArray

        restoreAmount = mangasJson.size() + 1 // +1 for categories

        // Restore categories
        json.get(CATEGORIES)?.let {
            backupManager.restoreCategories(it.asJsonArray)
            restoreProgress += 1
            showRestoreProgress(restoreProgress, restoreAmount, "Categories added", errors.size)
        }

        return Observable.from(mangasJson)
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
        val source = backupManager.sourceManager.get(manga.source) ?: return null
        val dbManga = backupManager.getMangaFromDatabase(manga)

        if (dbManga == null) {
            // Manga not in database
            return mangaFetchObservable(source, manga, chapters, categories, history, tracks)
        } else { // Manga in database
            // Copy information from manga already in database
            backupManager.restoreMangaNoFetch(manga, dbManga)
            // Fetch rest of manga information
            return mangaNoFetchObservable(source, manga, chapters, categories, history, tracks)
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
                .flatMap { manga ->
                    chapterFetchObservable(source, manga, chapters)
                            // Convert to the manga that contains new chapters.
                            .map { manga }
                }
                .doOnNext {
                    // Restore categories
                    backupManager.restoreCategoriesForManga(it, categories)

                    // Restore history
                    backupManager.restoreHistoryForManga(history)

                    // Restore tracking
                    backupManager.restoreTrackForManga(it, tracks)
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
                    // Restore categories
                    backupManager.restoreCategoriesForManga(it, categories)

                    // Restore history
                    backupManager.restoreHistoryForManga(history)

                    // Restore tracking
                    backupManager.restoreTrackForManga(it, tracks)
                }
                .doOnCompleted {
                    restoreProgress += 1
                    showRestoreProgress(restoreProgress, restoreAmount, backupManga.title, errors.size)
                }
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
                    Pair(emptyList<Chapter>(), emptyList<Chapter>())
                }
    }


    /**
     * Called to update dialog in [SettingsBackupFragment]
     *
     * @param progress restore progress
     * @param amount total restoreAmount of manga
     * @param title title of restored manga
     */
    private fun showRestoreProgress(progress: Int, amount: Int, title: String, errors: Int,
                                    content: String = getString(R.string.dialog_restoring_backup, title.chop(15))) {
        val intent = Intent(SettingsBackupFragment.INTENT_FILTER).apply {
            putExtra(SettingsBackupFragment.EXTRA_PROGRESS, progress)
            putExtra(SettingsBackupFragment.EXTRA_AMOUNT, amount)
            putExtra(SettingsBackupFragment.EXTRA_CONTENT, content)
            putExtra(SettingsBackupFragment.EXTRA_ERRORS, errors)
            putExtra(SettingsBackupFragment.ACTION, SettingsBackupFragment.ACTION_SET_PROGRESS_DIALOG)
        }
        sendLocalBroadcast(intent)
    }

}