package eu.kanade.tachiyomi.data.library

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.app.NotificationCompat
import eu.kanade.tachiyomi.Constants
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadService
import eu.kanade.tachiyomi.data.library.LibraryUpdateService.Companion.start
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.*
import rx.Observable
import rx.Subscription
import rx.schedulers.Schedulers
import uy.kohesive.injekt.injectLazy
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * This class will take care of updating the chapters of the manga from the library. It can be
 * started calling the [start] method. If it's already running, it won't do anything.
 * While the library is updating, a [PowerManager.WakeLock] will be held until the update is
 * completed, preventing the device from going to sleep mode. A notification will display the
 * progress of the update, and if case of an unexpected error, this service will be silently
 * destroyed.
 */
class LibraryUpdateService : Service() {

    /**
     * Database helper.
     */
    val db: DatabaseHelper by injectLazy()

    /**
     * Source manager.
     */
    val sourceManager: SourceManager by injectLazy()

    /**
     * Preferences.
     */
    val preferences: PreferencesHelper by injectLazy()

    val downloadManager: DownloadManager by injectLazy()

    /**
     * Wake lock that will be held until the service is destroyed.
     */
    private lateinit var wakeLock: PowerManager.WakeLock

    /**
     * Subscription where the update is done.
     */
    private var subscription: Subscription? = null

    /**
     * Pending intent of action that cancels the library update
     */
    private val cancelPendingIntent by lazy {NotificationReceiver.cancelLibraryUpdatePendingBroadcast(this)}

    /**
     * Id of the library update notification.
     */
    private val notificationId: Int
        get() = Constants.NOTIFICATION_LIBRARY_ID

    private val notificationBitmap by lazy {
        BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
    }

    companion object {

        /**
         * Key for category to update.
         */
        const val UPDATE_CATEGORY = "category"

        /**
         * Key for updating the details instead of the chapters.
         */
        const val UPDATE_DETAILS = "details"

        /**
         * Returns the status of the service.
         *
         * @param context the application context.
         * @return true if the service is running, false otherwise.
         */
        fun isRunning(context: Context): Boolean {
            return AndroidComponentUtil.isServiceRunning(context, LibraryUpdateService::class.java)
        }

        /**
         * Starts the service. It will be started only if there isn't another instance already
         * running.
         *
         * @param context the application context.
         * @param category a specific category to update, or null for global update.
         * @param details whether to update the details instead of the list of chapters.
         */
        fun start(context: Context, category: Category? = null, details: Boolean = false) {
            if (!isRunning(context)) {
                val intent = Intent(context, LibraryUpdateService::class.java).apply {
                    putExtra(UPDATE_DETAILS, details)
                    category?.let { putExtra(UPDATE_CATEGORY, it.id) }
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
            context.stopService(Intent(context, LibraryUpdateService::class.java))
        }

    }

    /**
     * Method called when the service is created. It injects dagger dependencies and acquire
     * the wake lock.
     */
    override fun onCreate() {
        super.onCreate()
        createAndAcquireWakeLock()
    }

    /**
     * Method called when the service is destroyed. It destroys the running subscription, resets
     * the alarm and release the wake lock.
     */
    override fun onDestroy() {
        subscription?.unsubscribe()
        destroyWakeLock()
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

        // Update favorite manga. Destroy service when completed or in case of an error.
        subscription = Observable
                .defer {
                    val mangaList = getMangaToUpdate(intent)

                    // Update either chapter list or manga details.
                    if (!intent.getBooleanExtra(UPDATE_DETAILS, false))
                        updateChapterList(mangaList)
                    else
                        updateDetails(mangaList)
                }
                .subscribeOn(Schedulers.io())
                .subscribe({
                }, {
                    showNotification(getString(R.string.notification_update_error), "")
                    stopSelf(startId)
                }, {
                    stopSelf(startId)
                })

        return Service.START_REDELIVER_INTENT
    }

    /**
     * Returns the list of manga to be updated.
     *
     * @param intent the update intent.
     * @return a list of manga to update
     */
    fun getMangaToUpdate(intent: Intent): List<Manga> {
        val categoryId = intent.getIntExtra(UPDATE_CATEGORY, -1)

        var listToUpdate = if (categoryId != -1)
            db.getLibraryMangas().executeAsBlocking().filter { it.category == categoryId }
        else {
            val categoriesToUpdate = preferences.libraryUpdateCategories().getOrDefault().map { it.toInt() }
            if (categoriesToUpdate.isNotEmpty())
                db.getLibraryMangas().executeAsBlocking()
                        .filter { it.category in categoriesToUpdate }
                        .distinctBy { it.id }
            else
                db.getFavoriteMangas().executeAsBlocking().distinctBy { it.id }
        }

        if (!intent.getBooleanExtra(UPDATE_DETAILS, false) && preferences.updateOnlyNonCompleted()) {
            listToUpdate = listToUpdate.filter { it.status != SManga.COMPLETED }
        }

        return listToUpdate
    }

    /**
     * Method that updates the given list of manga. It's called in a background thread, so it's safe
     * to do heavy operations or network calls here.
     * For each manga it calls [updateManga] and updates the notification showing the current
     * progress.
     *
     * @param mangaToUpdate the list to update
     * @return an observable delivering the progress of each update.
     */
    fun updateChapterList(mangaToUpdate: List<Manga>): Observable<Manga> {
        // Initialize the variables holding the progress of the updates.
        val count = AtomicInteger(0)
        val newUpdates = ArrayList<Manga>()
        val failedUpdates = ArrayList<Manga>()

        // Emit each manga and update it sequentially.
        return Observable.from(mangaToUpdate)
                // Notify manga that will update.
                .doOnNext { showProgressNotification(it, count.andIncrement, mangaToUpdate.size, cancelPendingIntent) }
                // Update the chapters of the manga.
                .concatMap { manga ->
                    updateManga(manga)
                            // If there's any error, return empty update and continue.
                            .onErrorReturn {
                                failedUpdates.add(manga)
                                Pair(emptyList<Chapter>(), emptyList<Chapter>())
                            }
                            // Filter out mangas without new chapters (or failed).
                            .filter { pair -> pair.first.size > 0 }
                            .doOnNext {
                                if (preferences.downloadNew()) {
                                    downloadChapters(manga, it.first)
                                }
                            }
                            // Convert to the manga that contains new chapters.
                            .map { manga }
                }
                // Add manga with new chapters to the list.
                .doOnNext { manga ->
                    // Set last updated time
                    manga.last_update = Date().time
                    db.updateLastUpdated(manga).executeAsBlocking()
                    // Add to the list
                    newUpdates.add(manga)
                }
                // Notify result of the overall update.
                .doOnCompleted {
                    if (newUpdates.isEmpty()) {
                        cancelNotification()
                    } else {
                        if (preferences.downloadNew()) {
                            DownloadService.start(this)
                        }
                        showResultNotification(newUpdates, failedUpdates)
                    }
                }
    }

    fun downloadChapters(manga: Manga, chapters: List<Chapter>) {
        // we need to get the chapters from the db so we have chapter ids
        val mangaChapters = db.getChapters(manga).executeAsBlocking()
        val dbChapters = chapters.map {
            mangaChapters.find { mangaChapter -> mangaChapter.url == it.url }!!
        }
        downloadManager.downloadChapters(manga, dbChapters)
    }

    /**
     * Updates the chapters for the given manga and adds them to the database.
     *
     * @param manga the manga to update.
     * @return a pair of the inserted and removed chapters.
     */
    fun updateManga(manga: Manga): Observable<Pair<List<Chapter>, List<Chapter>>> {
        val source = sourceManager.get(manga.source) as? HttpSource ?: return Observable.empty()
        return source.fetchChapterList(manga)
                .map { syncChaptersWithSource(db, it, manga, source) }
    }

    /**
     * Method that updates the details of the given list of manga. It's called in a background
     * thread, so it's safe to do heavy operations or network calls here.
     * For each manga it calls [updateManga] and updates the notification showing the current
     * progress.
     *
     * @param mangaToUpdate the list to update
     * @return an observable delivering the progress of each update.
     */
    fun updateDetails(mangaToUpdate: List<Manga>): Observable<Manga> {
        // Initialize the variables holding the progress of the updates.
        val count = AtomicInteger(0)

        // Emit each manga and update it sequentially.
        return Observable.from(mangaToUpdate)
                // Notify manga that will update.
                .doOnNext { showProgressNotification(it, count.andIncrement, mangaToUpdate.size, cancelPendingIntent) }
                // Update the details of the manga.
                .concatMap { manga ->
                    val source = sourceManager.get(manga.source) as? HttpSource
                            ?: return@concatMap Observable.empty<Manga>()

                    source.fetchMangaDetails(manga)
                            .map { networkManga ->
                                manga.copyFrom(networkManga)
                                db.insertManga(manga).executeAsBlocking()
                                manga
                            }
                            .onErrorReturn { manga }
                }
                .doOnCompleted {
                    cancelNotification()
                }
    }

    /**
     * Returns the text that will be displayed in the notification when there are new chapters.
     *
     * @param updates a list of manga that contains new chapters.
     * @param failedUpdates a list of manga that failed to update.
     * @return the body of the notification to display.
     */
    private fun getUpdatedMangasBody(updates: List<Manga>, failedUpdates: List<Manga>): String {
        return buildString {
            if (updates.isEmpty()) {
                append(getString(R.string.notification_no_new_chapters))
                append("\n")
            } else {
                append(getString(R.string.notification_new_chapters))
                for (manga in updates) {
                    append("\n")
                    append(manga.title.chop(45))
                }
            }
            if (!failedUpdates.isEmpty()) {
                append("\n\n")
                append(getString(R.string.notification_manga_update_failed))
                for (manga in failedUpdates) {
                    append("\n")
                    append(manga.title.chop(45))
                }
            }
        }
    }

    /**
     * Creates and acquires a wake lock until the library is updated.
     */
    private fun createAndAcquireWakeLock() {
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, "LibraryUpdateService:WakeLock")
        wakeLock.acquire()
    }

    /**
     * Releases the wake lock if it's held.
     */
    private fun destroyWakeLock() {
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    /**
     * Shows the notification with the given title and body.
     *
     * @param title the title of the notification.
     * @param body the body of the notification.
     */
    private fun showNotification(title: String, body: String) {
        notificationManager.notify(notificationId, notification {
            setSmallIcon(R.drawable.ic_refresh_white_24dp_img)
            setLargeIcon(notificationBitmap)
            setContentTitle(title)
            setContentText(body)
        })
    }

    /**
     * Shows the notification containing the currently updating manga and the progress.
     *
     * @param manga the manga that's being updated.
     * @param current the current progress.
     * @param total the total progress.
     */
    private fun showProgressNotification(manga: Manga, current: Int, total: Int, cancelIntent: PendingIntent) {
        notificationManager.notify(notificationId, notification {
            setSmallIcon(R.drawable.ic_refresh_white_24dp_img)
            setLargeIcon(notificationBitmap)
            setContentTitle(manga.title)
            setProgress(total, current, false)
            setOngoing(true)
            addAction(R.drawable.ic_clear_grey_24dp_img, getString(android.R.string.cancel), cancelIntent)
        })
    }


    /**
     * Shows the notification containing the result of the update done by the service.
     *
     * @param updates a list of manga with new updates.
     * @param failed a list of manga that failed to update.
     */
    private fun showResultNotification(updates: List<Manga>, failed: List<Manga>) {
        val title = getString(R.string.notification_update_completed)
        val body = getUpdatedMangasBody(updates, failed)

        notificationManager.notify(notificationId, notification {
            setSmallIcon(R.drawable.ic_refresh_white_24dp_img)
            setLargeIcon(notificationBitmap)
            setContentTitle(title)
            setStyle(NotificationCompat.BigTextStyle().bigText(body))
            setContentIntent(notificationIntent)
            setAutoCancel(true)
        })
    }

    /**
     * Cancels the notification.
     */
    private fun cancelNotification() {
        notificationManager.cancel(notificationId)
    }

    /**
     * Property that returns an intent to open the main activity.
     */
    private val notificationIntent: PendingIntent
        get() {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
}
