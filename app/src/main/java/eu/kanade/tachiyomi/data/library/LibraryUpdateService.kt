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
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.GROUP_ALERT_SUMMARY
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadService
import eu.kanade.tachiyomi.data.library.LibraryUpdateRanker.rankingScheme
import eu.kanade.tachiyomi.data.library.LibraryUpdateService.Companion.start
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.chapter.syncChaptersWithSource
import eu.kanade.tachiyomi.util.lang.chop
import eu.kanade.tachiyomi.util.system.isServiceRunning
import eu.kanade.tachiyomi.util.system.notification
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notificationManager
import rx.Observable
import rx.Subscription
import rx.schedulers.Schedulers
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.ArrayList
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
        val downloadManager: DownloadManager = Injekt.get(),
        val trackManager: TrackManager = Injekt.get()
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
    private val progressNotificationBuilder by lazy {
        notificationBuilder(Notifications.CHANNEL_LIBRARY) {
            setContentTitle(getString(R.string.app_name))
            setSmallIcon(R.drawable.ic_refresh_white_24dp)
            setLargeIcon(notificationBitmap)
            setOngoing(true)
            setOnlyAlertOnce(true)
            addAction(R.drawable.ic_close_white_24dp, getString(android.R.string.cancel), cancelIntent)
        }
    }

    /**
     * Defines what should be updated within a service execution.
     */
    enum class Target {
        CHAPTERS, // Manga chapters
        DETAILS,  // Manga metadata
        TRACKING  // Tracking metadata
    }

    companion object {

        /**
         * Key for category to update.
         */
        const val KEY_CATEGORY = "category"

        /**
         * Key that defines what should be updated.
         */
        const val KEY_TARGET = "target"

        /**
         * Returns the status of the service.
         *
         * @param context the application context.
         * @return true if the service is running, false otherwise.
         */
        fun isRunning(context: Context): Boolean {
            return context.isServiceRunning(LibraryUpdateService::class.java)
        }

        /**
         * Starts the service. It will be started only if there isn't another instance already
         * running.
         *
         * @param context the application context.
         * @param category a specific category to update, or null for global update.
         * @param target defines what should be updated.
         */
        fun start(context: Context, category: Category? = null, target: Target = Target.CHAPTERS) {
            if (!isRunning(context)) {
                val intent = Intent(context, LibraryUpdateService::class.java).apply {
                    putExtra(KEY_TARGET, target)
                    category?.let { putExtra(KEY_CATEGORY, it.id) }
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
            context.stopService(Intent(context, LibraryUpdateService::class.java))
        }

    }

    /**
     * Method called when the service is created. It injects dagger dependencies and acquire
     * the wake lock.
     */
    override fun onCreate() {
        super.onCreate()
        startForeground(Notifications.ID_LIBRARY_PROGRESS, progressNotificationBuilder.build())
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
        if (intent == null) return START_NOT_STICKY
        val target = intent.getSerializableExtra(KEY_TARGET) as? Target
                ?: return START_NOT_STICKY

        // Unsubscribe from any previous subscription if needed.
        subscription?.unsubscribe()

        // Update favorite manga. Destroy service when completed or in case of an error.
        subscription = Observable
                .defer {
                    val selectedScheme = preferences.libraryUpdatePrioritization().getOrDefault()
                    val mangaList = getMangaToUpdate(intent, target)
                            .sortedWith(rankingScheme[selectedScheme])

                    // Update either chapter list or manga details.
                    when (target) {
                        Target.CHAPTERS -> updateChapterList(mangaList)
                        Target.DETAILS -> updateDetails(mangaList)
                        Target.TRACKING -> updateTrackings(mangaList)
                    }
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
     * @param target the target to update.
     * @return a list of manga to update
     */
    fun getMangaToUpdate(intent: Intent, target: Target): List<LibraryManga> {
        val categoryId = intent.getIntExtra(KEY_CATEGORY, -1)

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
        if (target == Target.CHAPTERS && preferences.updateOnlyNonCompleted()) {
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
    fun updateChapterList(mangaToUpdate: List<LibraryManga>): Observable<LibraryManga> {
        // Initialize the variables holding the progress of the updates.
        val count = AtomicInteger(0)
        // List containing new updates
        val newUpdates = ArrayList<Pair<LibraryManga, Array<Chapter>>>()
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
                                Pair(emptyList(), emptyList())
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
                            .map { Pair(manga, (it.first.sortedByDescending { ch -> ch
                                .source_order }.toTypedArray())) }
                }
                // Add manga with new chapters to the list.
                .doOnNext { manga ->
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
                .map { manga -> manga.first }
    }

    fun downloadChapters(manga: Manga, chapters: List<Chapter>) {
        // we need to get the chapters from the db so we have chapter ids
        val mangaChapters = db.getChapters(manga).executeAsBlocking()
        val dbChapters = chapters.map {
            mangaChapters.find { mangaChapter -> mangaChapter.url == it.url }!!
        }
        // We don't want to start downloading while the library is updating, because websites
        // may don't like it and they could ban the user.
        downloadManager.downloadChapters(manga, dbChapters, false)
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
     *
     * @param mangaToUpdate the list to update
     * @return an observable delivering the progress of each update.
     */
    fun updateDetails(mangaToUpdate: List<LibraryManga>): Observable<LibraryManga> {
        // Initialize the variables holding the progress of the updates.
        val count = AtomicInteger(0)

        // Emit each manga and update it sequentially.
        return Observable.from(mangaToUpdate)
                // Notify manga that will update.
                .doOnNext { showProgressNotification(it, count.andIncrement, mangaToUpdate.size) }
                // Update the details of the manga.
                .concatMap { manga ->
                    val source = sourceManager.get(manga.source) as? HttpSource
                            ?: return@concatMap Observable.empty<LibraryManga>()

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
     * Method that updates the metadata of the connected tracking services. It's called in a
     * background thread, so it's safe to do heavy operations or network calls here.
     */
    private fun updateTrackings(mangaToUpdate: List<LibraryManga>): Observable<LibraryManga> {
        // Initialize the variables holding the progress of the updates.
        var count = 0

        val loggedServices = trackManager.services.filter { it.isLogged }

        // Emit each manga and update it sequentially.
        return Observable.from(mangaToUpdate)
                // Notify manga that will update.
                .doOnNext { showProgressNotification(it, count++, mangaToUpdate.size) }
                // Update the tracking details.
                .concatMap { manga ->
                    val tracks = db.getTracks(manga).executeAsBlocking()

                    Observable.from(tracks)
                            .concatMap { track ->
                                val service = trackManager.getService(track.sync_id)
                                if (service != null && service in loggedServices) {
                                    service.refresh(track)
                                            .doOnNext { db.insertTrack(it).executeAsBlocking() }
                                            .onErrorReturn { track }
                                } else {
                                    Observable.empty()
                                }
                            }
                            .map { manga }
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
        notificationManager.notify(Notifications.ID_LIBRARY_PROGRESS, progressNotificationBuilder
                .setContentTitle(manga.title)
                .setProgress(total, current, false)
                .build())
    }

    /**
     * Shows the notification containing the result of the update done by the service.
     *
     * @param updates a list of manga with new updates.
     */
    private fun showResultNotification(updates: List<Pair<Manga, Array<Chapter>>>) {
        val notifications = ArrayList<Pair<Notification, Int>>()
        updates.forEach {
            val manga = it.first
            val chapters = it.second
            val chapterNames = chapters.map { chapter -> chapter.name }.toSet()
            notifications.add(Pair(notification(Notifications.CHANNEL_NEW_CHAPTERS) {
                setSmallIcon(R.drawable.ic_tachi)
                try {
                    val icon = Glide.with(this@LibraryUpdateService)
                        .asBitmap().load(manga).dontTransform().centerCrop().circleCrop()
                        .override(256, 256).submit().get()
                    setLargeIcon(icon)
                }
                catch (e: Exception) { }
                setGroupAlertBehavior(GROUP_ALERT_SUMMARY)
                setContentTitle(manga.title)
                val chaptersNames = if (chapterNames.size > 5) {
                    "${chapterNames.take(4).joinToString(", ")}, " +
                        resources.getQuantityString(R.plurals.notification_and_n_more,
                            (chapterNames.size - 4), (chapterNames.size - 4))
                } else chapterNames.joinToString(", ")
                setContentText(chaptersNames)
                setStyle(NotificationCompat.BigTextStyle().bigText(chaptersNames))
                priority = NotificationCompat.PRIORITY_HIGH
                setGroup(Notifications.GROUP_NEW_CHAPTERS)
                setContentIntent(
                    NotificationReceiver.openChapterPendingActivity(
                        this@LibraryUpdateService, manga, chapters.first()
                    )
                )
                addAction(R.drawable.ic_glasses_black_24dp, getString(R.string.action_mark_as_read),
                    NotificationReceiver.markAsReadPendingBroadcast(this@LibraryUpdateService,
                        manga, chapters, Notifications.ID_NEW_CHAPTERS))
                addAction(R.drawable.ic_book_white_24dp, getString(R.string.action_view_chapters),
                    NotificationReceiver.openChapterPendingActivity(this@LibraryUpdateService,
                        manga, Notifications.ID_NEW_CHAPTERS))
                setAutoCancel(true)
            }, manga.id.hashCode()))
        }

        NotificationManagerCompat.from(this).apply {

            notify(Notifications.ID_NEW_CHAPTERS, notification(Notifications.CHANNEL_NEW_CHAPTERS) {
                setSmallIcon(R.drawable.ic_tachi)
                setLargeIcon(notificationBitmap)
                setContentTitle(getString(R.string.notification_new_chapters))
                if (updates.size > 1) {
                    setContentText(resources.getQuantityString(R.plurals
                        .notification_new_chapters_text,
                        updates.size, updates.size))
                    setStyle(NotificationCompat.BigTextStyle().bigText(updates.joinToString("\n") {
                        it.first.title.chop(45)
                    }))
                }
                else {
                    setContentText(updates.first().first.title.chop(45))
                }
                priority = NotificationCompat.PRIORITY_HIGH
                setGroup(Notifications.GROUP_NEW_CHAPTERS)
                setGroupAlertBehavior(GROUP_ALERT_SUMMARY)
                setGroupSummary(true)
                setContentIntent(getNotificationIntent())
                setAutoCancel(true)
            })

            notifications.forEach {
                notify(it.second, it.first)
            }
        }
    }

    /**
     * Cancels the progress notification.
     */
    private fun cancelProgressNotification() {
        notificationManager.cancel(Notifications.ID_LIBRARY_PROGRESS)
    }

    /**
     * Returns an intent to open the main activity.
     */
    private fun getNotificationIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        intent.action = MainActivity.SHORTCUT_RECENTLY_UPDATED
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

}
