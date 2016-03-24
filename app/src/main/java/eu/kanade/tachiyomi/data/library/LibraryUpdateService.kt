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
import eu.kanade.tachiyomi.App
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.source.SourceManager
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.AndroidComponentUtil
import eu.kanade.tachiyomi.util.DeviceUtil
import eu.kanade.tachiyomi.util.notification
import eu.kanade.tachiyomi.util.notificationManager
import rx.Observable
import rx.Subscription
import rx.schedulers.Schedulers
import timber.log.Timber
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

// Intent key for forced library update
val UPDATE_IS_FORCED = "is_forced"

/**
 * Get the start intent for [LibraryUpdateService].
 * @param context the application context.
 * @param isForced true when forcing library update
 * @return the intent of the service.
 */
fun getIntent(context: Context, isForced: Boolean = false): Intent {
    return Intent(context, LibraryUpdateService::class.java).apply {
        putExtra(UPDATE_IS_FORCED, isForced)
    }
}

/**
 * Returns the status of the service.
 * @param context the application context.
 * @return true if the service is running, false otherwise.
 */
fun isRunning(context: Context): Boolean {
    return AndroidComponentUtil.isServiceRunning(context, LibraryUpdateService::class.java)
}

/**
 * This class will take care of updating the chapters of the manga from the library. It can be
 * started calling the [start] method. If it's already running, it won't do anything.
 * While the library is updating, a [PowerManager.WakeLock] will be held until the update is
 * completed, preventing the device from going to sleep mode. A notification will display the
 * progress of the update, and if case of an unexpected error, this service will be silently
 * destroyed.
 */
class LibraryUpdateService : Service() {

    // Dependencies injected through dagger.
    @Inject lateinit var db: DatabaseHelper
    @Inject lateinit var sourceManager: SourceManager
    @Inject lateinit var preferences: PreferencesHelper

    // Wake lock that will be held until the service is destroyed.
    private lateinit var wakeLock: PowerManager.WakeLock

    // Subscription where the update is done.
    private var subscription: Subscription? = null


    companion object {
        val UPDATE_NOTIFICATION_ID = 1

        /**
         * Static method to start the service. It will be started only if there isn't another
         * instance already running.
         * @param context the application context.
         */
        @JvmStatic
        fun start(context: Context, isForced: Boolean = false) {
            if (!isRunning(context)) {
                context.startService(getIntent(context, isForced))
            }
        }

        fun stop(context: Context) {
            context.stopService(getIntent(context))
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
     * Method called when the service is destroyed. It destroy the running subscription, resets
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
     * Method called when the service receives an intent. In this case, the content of the intent
     * is irrelevant, because everything required is fetched in [updateLibrary].
     * @param intent the intent from [start].
     * @param flags the flags of the command.
     * @param startId the start id of this command.
     * @return the start value of the command.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // If there's no network available, set a component to start this service again when
        // a connection is available.
        if (!DeviceUtil.isNetworkConnected(this)) {
            Timber.i("Sync canceled, connection not available")
            showWarningNotification(getString(R.string.notification_no_connection_title),
                    getString(R.string.notification_no_connection_body))
            AndroidComponentUtil.toggleComponent(this, SyncOnConnectionAvailable::class.java, true)
            stopSelf(startId)
            return Service.START_NOT_STICKY
        }

        // If user doesn't want to update while phone is not charging, cancel sync
        else if (preferences.updateOnlyWhenCharging() && !(intent?.getBooleanExtra(UPDATE_IS_FORCED, false) ?: false) && !DeviceUtil.isPowerConnected(this)) {
            Timber.i("Sync canceled, not connected to ac power")
            // Create force library update intent
            val forceIntent = getLibraryUpdateReceiverIntent(LibraryUpdateReceiver.FORCE_LIBRARY_UPDATE)
            // Show warning
            showWarningNotification(getString(R.string.notification_not_connected_to_ac_title),
                    getString(R.string.notification_not_connected_to_ac_body), forceIntent)
            stopSelf(startId)
            return Service.START_NOT_STICKY
        }

        // Unsubscribe from any previous subscription if needed.
        subscription?.unsubscribe()

        // Update favorite manga. Destroy service when completed or in case of an error.
        subscription = Observable.defer { updateLibrary() }
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
     * Creates a PendingIntent for LibraryUpdate broadcast class
     * @param action id of action
     */
    fun getLibraryUpdateReceiverIntent(action: String): PendingIntent {
        return PendingIntent.getBroadcast(this, 0,
                Intent(this, LibraryUpdateReceiver::class.java).apply { this.action = action }, 0)
    }

    /**
     * Method that updates the library. It's called in a background thread, so it's safe to do
     * heavy operations or network calls here.
     * For each manga it calls [updateManga] and updates the notification showing the current
     * progress.
     * @return an observable delivering the progress of each update.
     */
    fun updateLibrary(): Observable<Manga> {
        // Initialize the variables holding the progress of the updates.
        val count = AtomicInteger(0)
        val newUpdates = ArrayList<Manga>()
        val failedUpdates = ArrayList<Manga>()

        val cancelIntent = getLibraryUpdateReceiverIntent(LibraryUpdateReceiver.CANCEL_LIBRARY_UPDATE)

        // Get the manga list that is going to be updated.
        val allLibraryMangas = db.getFavoriteMangas().executeAsBlocking()
        val toUpdate = if (!preferences.updateOnlyNonCompleted())
            allLibraryMangas
        else
            allLibraryMangas.filter { it.status != Manga.COMPLETED }

        // Emit each manga and update it sequentially.
        return Observable.from(toUpdate)
                // Notify manga that will update.
                .doOnNext { showProgressNotification(it, count.andIncrement, toUpdate.size, cancelIntent) }
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
     * @param title the title of the notification.
     * @param body the body of the notification.
     */
    private fun showNotification(title: String, body: String) {
        val n = notification() {
            setSmallIcon(R.drawable.ic_refresh_white_24dp_img)
            setContentTitle(title)
            setContentText(body)
        }
        notificationManager.notify(UPDATE_NOTIFICATION_ID, n)
    }

    /**
     * Shows the notification containing the currently updating manga and the progress.
     * @param manga the manga that's being updated.
     * @param current the current progress.
     * @param total the total progress.
     */
    private fun showProgressNotification(manga: Manga, current: Int, total: Int, cancelIntent: PendingIntent) {
        val n = notification() {
            setSmallIcon(R.drawable.ic_refresh_white_24dp_img)
            setContentTitle(manga.title)
            setProgress(total, current, false)
            setOngoing(true)
            addAction(R.drawable.ic_clear_grey_24dp_img, getString(android.R.string.cancel), cancelIntent)
        }
        notificationManager.notify(UPDATE_NOTIFICATION_ID, n)
    }

    /**
     * Show warning message when library can't be updated
     * @param warningTitle title of warning
     * @param warningBody warning information
     * @param pendingIntent Intent called when action clicked
     */
    private fun showWarningNotification(warningTitle: String, warningBody: String, pendingIntent: PendingIntent? = null) {
        val n = notification() {
            setSmallIcon(R.drawable.ic_warning_white_24dp_img)
            setContentTitle(warningTitle)
            setStyle(NotificationCompat.BigTextStyle().bigText(warningBody))
            setContentIntent(notificationIntent)
            if (pendingIntent != null) {
                addAction(R.drawable.ic_refresh_grey_24dp_img, getString(R.string.action_force), pendingIntent)
            }
            setAutoCancel(true)
        }
        notificationManager.notify(UPDATE_NOTIFICATION_ID, n)
    }

    /**
     * Shows the notification containing the result of the update done by the service.
     * @param updates a list of manga with new updates.
     * @param failed a list of manga that failed to update.
     */
    private fun showResultNotification(updates: List<Manga>, failed: List<Manga>) {
        val title = getString(R.string.notification_update_completed)
        val body = getUpdatedMangasBody(updates, failed)

        val n = notification() {
            setSmallIcon(R.drawable.ic_refresh_white_24dp_img)
            setContentTitle(title)
            setStyle(NotificationCompat.BigTextStyle().bigText(body))
            setContentIntent(notificationIntent)
            setAutoCancel(true)
        }
        notificationManager.notify(UPDATE_NOTIFICATION_ID, n)
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
         * @param context the application context.
         * @param intent the intent received.
         */
        override fun onReceive(context: Context, intent: Intent) {
            if (DeviceUtil.isNetworkConnected(context)) {
                AndroidComponentUtil.toggleComponent(context, this.javaClass, false)
                context.startService(getIntent(context))
            }
        }
    }

    /**
     * Class that triggers the library to update.
     */
    class LibraryUpdateReceiver : BroadcastReceiver() {
        companion object {
            // Cancel library update action
            val CANCEL_LIBRARY_UPDATE = "eu.kanade.CANCEL_LIBRARY_UPDATE"
            // Force library update
            val FORCE_LIBRARY_UPDATE = "eu.kanade.FORCE_LIBRARY_UPDATE"
        }

        /**
         * Method called when user wants a library update.
         * @param context the application context.
         * @param intent the intent received.
         */
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                CANCEL_LIBRARY_UPDATE -> {
                    LibraryUpdateService.stop(context)
                    context.notificationManager.cancel(UPDATE_NOTIFICATION_ID)
                }
                FORCE_LIBRARY_UPDATE -> LibraryUpdateService.start(context, true)
            }
        }
    }

}
