package eu.kanade.tachiyomi.data.library

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
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
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
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
class LibraryUpdateService(
        val db: DatabaseHelper = Injekt.get(),
        val sourceManager: SourceManager = Injekt.get(),
        val preferences: PreferencesHelper = Injekt.get(),
        val downloadManager: DownloadManager = Injekt.get()
) : Service() {

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
    private val cancelIntent by lazy {
        NotificationReceiver.cancelLibraryUpdatePendingBroadcast(this)
    }

    /**
     * Bitmap of the app for notifications.
     */
    private val notificationBitmap by lazy {
        BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
    }

    /**
     * Cached progress notification to avoid creating a lot.
     */
    private val progressNotification by lazy { NotificationCompat.Builder(this)
            .setSmallIcon(R.drawable.ic_refresh_white_24dp_img)
            .setLargeIcon(notificationBitmap)
            .setOngoing(true)
            .addAction(R.drawable.ic_clear_grey_24dp_img, getString(android.R.string.cancel), cancelIntent)
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
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, "LibraryUpdateService:WakeLock")
        wakeLock.acquire()
    }

    /**
     * Method called when the service is destroyed. It destroys subscriptions and releases the wake
     * lock.
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
                    Timber.e(it)
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
            val categoriesToUpdate = preferences.libraryUpdateCategories().getOrDefault().map(String::toInt)
            if (categoriesToUpdate.isNotEmpty())
                db.getLibraryMangas().executeAsBlocking()
                        .filter { it.category in categoriesToUpdate }
                        .distinctBy { it.id }
            else
                db.getLibraryMangas().executeAsBlocking().distinctBy { it.id }
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
        // List containing new updates
        val newUpdates = ArrayList<Manga>()
        // list containing failed updates
        val failedUpdates = ArrayList<Manga>()
        // List containing categories that get included in downloads.
        val categoriesToDownload = preferences.downloadNewCategories().getOrDefault().map(String::toInt)
        // Boolean to determine if user wants to automatically download new chapters.
        val downloadNew = preferences.downloadNew().getOrDefault()
        // Boolean to determine if DownloadManager has downloads
        var hasDownloads = false

        // Emit each manga and update it sequentially.
        return Observable.from(mangaToUpdate)
                // Notify manga that will update.
                .doOnNext { showProgressNotification(it, count.andIncrement, mangaToUpdate.size) }
                // Update the chapters of the manga.
                .concatMap { manga ->
                    updateManga(manga)
                            // If there's any error, return empty update and continue.
                            .onErrorReturn {
                                failedUpdates.add(manga)
                                Pair(emptyList<Chapter>(), emptyList<Chapter>())
                            }
                            // Filter out mangas without new chapters (or failed).
                            .filter { pair -> pair.first.isNotEmpty() }
                            .doOnNext {
                                if (downloadNew && (categoriesToDownload.isEmpty() ||
                                        manga.category in categoriesToDownload)) {

                                    downloadChapters(manga, it.first)
                                    hasDownloads = true
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
                    if (newUpdates.isNotEmpty()) {
                        showResultNotification(newUpdates)
                        if (downloadNew && hasDownloads) {
                            DownloadService.start(this)
                        }
                    }

                    if (failedUpdates.isNotEmpty()) {
                        Timber.e("Failed updating: ${failedUpdates.map { it.title }}")
                    }

                    cancelProgressNotification()
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
                .doOnNext { showProgressNotification(it, count.andIncrement, mangaToUpdate.size) }
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
                    cancelProgressNotification()
                }
    }

    /**
     * Shows the notification containing the currently updating manga and the progress.
     *
     * @param manga the manga that's being updated.
     * @param current the current progress.
     * @param total the total progress.
     */
    private fun showProgressNotification(manga: Manga, current: Int, total: Int) {
        notificationManager.notify(Constants.NOTIFICATION_LIBRARY_PROGRESS_ID, progressNotification
                .setContentTitle(manga.title)
                .setProgress(total, current, false)
                .build())
    }

    /**
     * Shows the notification containing the result of the update done by the service.
     *
     * @param updates a list of manga with new updates.
     */
    private fun showResultNotification(updates: List<Manga>) {
        val newUpdates = updates.map { it.title.chop(45) }.toMutableSet()

        // Append new chapters from a previous, existing notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val previousNotification = notificationManager.activeNotifications
                    .find { it.id == Constants.NOTIFICATION_LIBRARY_RESULT_ID }

            if (previousNotification != null) {
                val oldUpdates = previousNotification.notification.extras
                        .getString(Notification.EXTRA_BIG_TEXT)

                if (!oldUpdates.isNullOrEmpty()) {
                    newUpdates += oldUpdates.split("\n")
                }
            }
        }

        notificationManager.notify(Constants.NOTIFICATION_LIBRARY_RESULT_ID, notification {
            setSmallIcon(R.drawable.ic_book_white_24dp)
            setLargeIcon(notificationBitmap)
            setContentTitle(getString(R.string.notification_new_chapters))
            if (newUpdates.size > 1) {
                setContentText(getString(R.string.notification_new_chapters_text, newUpdates.size))
                setStyle(NotificationCompat.BigTextStyle().bigText(newUpdates.joinToString("\n")))
            } else {
                setContentText(newUpdates.first())
            }
            priority = NotificationCompat.PRIORITY_HIGH
            setContentIntent(getNotificationIntent())
            setAutoCancel(true)
        })
    }

    /**
     * Cancels the progress notification.
     */
    private fun cancelProgressNotification() {
        notificationManager.cancel(Constants.NOTIFICATION_LIBRARY_PROGRESS_ID)
    }

    /**
     * Returns an intent to open the main activity.
     */
    private fun getNotificationIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

}
