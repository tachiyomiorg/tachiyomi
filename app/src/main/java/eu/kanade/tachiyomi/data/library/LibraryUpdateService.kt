package eu.kanade.tachiyomi.data.library

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.app.NotificationCompat
import android.util.Pair
import com.github.pwittchen.reactivenetwork.library.ConnectivityStatus
import com.github.pwittchen.reactivenetwork.library.ReactiveNetwork
import eu.kanade.tachiyomi.App
import eu.kanade.tachiyomi.Constants
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.source.SourceManager
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.*
import rx.Observable
import rx.Subscription
import rx.schedulers.Schedulers
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

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
    @Inject lateinit var db: DatabaseHelper

    /**
     * Source manager.
     */
    @Inject lateinit var sourceManager: SourceManager

    /**
     * Preferences.
     */
    @Inject lateinit var preferences: PreferencesHelper

    /**
     * Wake lock that will be held until the service is destroyed.
     */
    private lateinit var wakeLock: PowerManager.WakeLock

    /**
     * Subscription where the update is done.
     */
    private var subscription: Subscription? = null


    companion object {
        /**
         * Id of the library update notification.
         */
        const val UPDATE_NOTIFICATION_ID = Constants.NOTIFICATION_LIBRARY_ID

        /**
         * Key for manual library update.
         */
        const val UPDATE_IS_MANUAL = "is_manual"

        /**
         * Key for category to update.
         */
        const val UPDATE_CATEGORY = "category"

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
         * @param isManual whether the update has been manually triggered.
         * @param category a specific category to update, or null for all in the library.
         */
        fun start(context: Context, isManual: Boolean = false, category: Category? = null) {
            if (!isRunning(context)) {
                val intent = Intent(context, LibraryUpdateService::class.java).apply {
                    putExtra(UPDATE_IS_MANUAL, isManual)
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
        App.get(this).component.inject(this)
        createAndAcquireWakeLock()
    }

    /**
     * Method called when the service is destroyed. It destroys the running subscription, resets
     * the alarm and release the wake lock.
     */
    override fun onDestroy() {
        subscription?.unsubscribe()
        LibraryUpdateAlarm.startAlarm(this)
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

        // Get connectivity status
        val connection = ReactiveNetwork().getConnectivityStatus(this, true)

        // Get library update restrictions
        val restrictions = preferences.libraryUpdateRestriction()

        // Check if users updates library manual
        val isManualUpdate = intent?.getBooleanExtra(UPDATE_IS_MANUAL, false) ?: false

        // Whether to cancel the update.
        var cancelUpdate = false

        // Check if device has internet connection
        // Check if device has wifi connection if only wifi is enabled
        if (connection == ConnectivityStatus.OFFLINE || (!isManualUpdate && "wifi" in restrictions
                && connection != ConnectivityStatus.WIFI_CONNECTED_HAS_INTERNET)) {

            if (isManualUpdate) {
                toast(R.string.notification_no_connection_title)
            }

            // Enable library update when connection available
            AndroidComponentUtil.toggleComponent(this, SyncOnConnectionAvailable::class.java, true)
            cancelUpdate = true
        }
        if (!isManualUpdate && "ac" in restrictions && !DeviceUtil.isPowerConnected(this)) {
            AndroidComponentUtil.toggleComponent(this, SyncOnPowerConnected::class.java, true)
            cancelUpdate = true
        }

        if (cancelUpdate) {
            stopSelf(startId)
            return Service.START_NOT_STICKY
        }

        // Stop enabled components.
        AndroidComponentUtil.toggleComponent(this, SyncOnConnectionAvailable::class.java, false)
        AndroidComponentUtil.toggleComponent(this, SyncOnPowerConnected::class.java, false)

        // Unsubscribe from any previous subscription if needed.
        subscription?.unsubscribe()

        // Update favorite manga. Destroy service when completed or in case of an error.
        subscription = Observable.defer { updateMangaList(getMangaToUpdate(intent)) }
                .subscribeOn(Schedulers.io())
                .subscribe({},
                        {
                            showNotification(getString(R.string.notification_update_error), "")
                            stopSelf(startId)
                        }, {
                    stopSelf(startId)
                })

        return Service.START_STICKY
    }

    /**
     * Returns the list of manga to be updated.
     *
     * @param intent the update intent.
     * @return a list of manga to update
     */
    fun getMangaToUpdate(intent: Intent?): List<Manga> {
        val categoryId = intent?.getIntExtra(UPDATE_CATEGORY, -1) ?: -1

        var toUpdate = if (categoryId != -1)
            db.getLibraryMangas().executeAsBlocking().filter { it.category == categoryId }
        else
            db.getFavoriteMangas().executeAsBlocking()

        if (preferences.updateOnlyNonCompleted()) {
            toUpdate = toUpdate.filter { it.status != Manga.COMPLETED }
        }

        return toUpdate
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
    fun updateMangaList(mangaToUpdate: List<Manga>): Observable<Manga> {
        // Initialize the variables holding the progress of the updates.
        val count = AtomicInteger(0)
        val newUpdates = ArrayList<Manga>()
        val failedUpdates = ArrayList<Manga>()

        val cancelIntent = PendingIntent.getBroadcast(this, 0,
                Intent(this, CancelUpdateReceiver::class.java), 0)

        // Emit each manga and update it sequentially.
        return Observable.from(mangaToUpdate)
                // Notify manga that will update.
                .doOnNext { showProgressNotification(it, count.andIncrement, mangaToUpdate.size, cancelIntent) }
                // Update the chapters of the manga.
                .concatMap { manga ->
                    updateManga(manga)
                            // If there's any error, return empty update and continue.
                            .onErrorReturn {
                                failedUpdates.add(manga)
                                Pair(0, 0)
                            }
                            // Filter out mangas without new chapters (or failed).
                            .filter { pair -> pair.first > 0 }
                            // Convert to the manga that contains new chapters.
                            .map { manga }
                }
                // Add manga with new chapters to the list.
                .doOnNext { newUpdates.add(it) }
                // Notify result of the overall update.
                .doOnCompleted {
                    if (newUpdates.isEmpty()) {
                        cancelNotification()
                    } else {
                        showResultNotification(newUpdates, failedUpdates)
                    }
                }
    }

    /**
     * Updates the chapters for the given manga and adds them to the database.
     *
     * @param manga the manga to update.
     * @return a pair of the inserted and removed chapters.
     */
    fun updateManga(manga: Manga): Observable<Pair<Int, Int>> {
        val source = sourceManager.get(manga.source)
        return source!!
                .pullChaptersFromNetwork(manga.url)
                .flatMap { db.insertOrRemoveChapters(manga, it, source) }
    }

    /**
     * Returns the text that will be displayed in the notification when there are new chapters.
     *
     * @param updates a list of manga that contains new chapters.
     * @param failedUpdates a list of manga that failed to update.
     * @return the body of the notification to display.
     */
    private fun getUpdatedMangasBody(updates: List<Manga>, failedUpdates: List<Manga>): String {
        return with(StringBuilder()) {
            if (updates.isEmpty()) {
                append(getString(R.string.notification_no_new_chapters))
                append("\n")
            } else {
                append(getString(R.string.notification_new_chapters))
                for (manga in updates) {
                    append("\n")
                    append(manga.title)
                }
            }
            if (!failedUpdates.isEmpty()) {
                append("\n\n")
                append(getString(R.string.notification_manga_update_failed))
                for (manga in failedUpdates) {
                    append("\n")
                    append(manga.title)
                }
            }
            toString()
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
        notificationManager.notify(UPDATE_NOTIFICATION_ID, notification() {
            setSmallIcon(R.drawable.ic_refresh_white_24dp_img)
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
        notificationManager.notify(UPDATE_NOTIFICATION_ID, notification() {
            setSmallIcon(R.drawable.ic_refresh_white_24dp_img)
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

        notificationManager.notify(UPDATE_NOTIFICATION_ID, notification() {
            setSmallIcon(R.drawable.ic_refresh_white_24dp_img)
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
        notificationManager.cancel(UPDATE_NOTIFICATION_ID)
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

    /**
     * Class that triggers the library to update when a connection is available. It receives
     * network changes.
     */
    class SyncOnConnectionAvailable : BroadcastReceiver() {
        /**
         * Method called when a network change occurs.
         *
         * @param context the application context.
         * @param intent the intent received.
         */
        override fun onReceive(context: Context, intent: Intent) {
            if (DeviceUtil.isNetworkConnected(context)) {
                AndroidComponentUtil.toggleComponent(context, this.javaClass, false)
                start(context)
            }
        }
    }

    /**
     * Class that triggers the library to update when connected to power.
     */
    class SyncOnPowerConnected: BroadcastReceiver() {
        /**
         * Method called when AC is connected.
         *
         * @param context the application context.
         * @param intent the intent received.
         */
        override fun onReceive(context: Context, intent: Intent) {
            AndroidComponentUtil.toggleComponent(context, this.javaClass, false)
            start(context)
        }
    }

    /**
     * Class that stops updating the library.
     */
    class CancelUpdateReceiver : BroadcastReceiver() {
        /**
         * Method called when user wants a library update.
         * @param context the application context.
         * @param intent the intent received.
         */
        override fun onReceive(context: Context, intent: Intent) {
            LibraryUpdateService.stop(context)
            context.notificationManager.cancel(UPDATE_NOTIFICATION_ID)
        }
    }
}
