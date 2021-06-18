package eu.kanade.tachiyomi.data.backup

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.os.PowerManager
import androidx.core.content.ContextCompat
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.backup.full.FullBackupRestore
import eu.kanade.tachiyomi.data.backup.legacy.LegacyBackupRestore
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.acquireWakeLock
import eu.kanade.tachiyomi.util.system.isServiceRunning
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Restores backup.
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
        fun start(context: Context, uri: Uri, mode: Int) {
            if (!isRunning(context)) {
                val intent = Intent(context, BackupRestoreService::class.java).apply {
                    putExtra(BackupConst.EXTRA_URI, uri)
                    putExtra(BackupConst.EXTRA_MODE, mode)
                }
                ContextCompat.startForegroundService(context, intent)
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

    private lateinit var ioScope: CoroutineScope
    private var backupRestore: AbstractBackupRestore<*>? = null
    private lateinit var notifier: BackupNotifier

    override fun onCreate() {
        super.onCreate()

        ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
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
        backupRestore?.job?.cancel()
        ioScope.cancel()
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
        val mode = intent.getIntExtra(BackupConst.EXTRA_MODE, BackupConst.BACKUP_TYPE_FULL)

        // Cancel any previous job if needed.
        backupRestore?.job?.cancel()

        backupRestore = when (mode) {
            BackupConst.BACKUP_TYPE_FULL -> FullBackupRestore(this, notifier)
            else -> LegacyBackupRestore(this, notifier)
        }

        val handler = CoroutineExceptionHandler { _, exception ->
            Timber.e(exception)
            backupRestore?.writeErrorLog()

            notifier.showRestoreError(exception.message)
            stopSelf(startId)
        }
        val job = ioScope.launch(handler) {
            if (backupRestore?.restoreBackup(uri) == false) {
                notifier.showRestoreError(getString(R.string.restoring_backup_canceled))
            }
        }
        job.invokeOnCompletion {
            stopSelf(startId)
        }
        backupRestore?.job = job

        return START_NOT_STICKY
    }
}
