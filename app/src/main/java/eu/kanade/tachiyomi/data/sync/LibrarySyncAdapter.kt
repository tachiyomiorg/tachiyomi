package eu.kanade.tachiyomi.data.sync

import android.accounts.Account
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.Context
import android.content.SyncResult
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v4.app.NotificationCompat
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.sync.account.SyncAccountAuthenticator
import eu.kanade.tachiyomi.data.sync.api.TWApi
import eu.kanade.tachiyomi.data.sync.protocol.ReportApplier
import eu.kanade.tachiyomi.data.sync.protocol.ReportGenerator
import eu.kanade.tachiyomi.util.accountManager
import eu.kanade.tachiyomi.util.notification
import eu.kanade.tachiyomi.util.notificationManager
import timber.log.Timber
import uy.kohesive.injekt.injectLazy
import java.io.IOException

/**
 * Core sync code
 */

class LibrarySyncAdapter(context: Context) : AbstractThreadedSyncAdapter(context, true, false) {
    
    private val db: DatabaseHelper by injectLazy()
    private val syncManager: LibrarySyncManager by injectLazy()
    
    private val reportGenerator by lazy { ReportGenerator(context) }
    private val reportApplier by lazy { ReportApplier(context) }
    
    /**
     * Bitmap of the app for notifications.
     */
    private val notificationBitmap by lazy {
        BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
    }
    
    /**
     * Cached progress notification to avoid creating a lot.
     */
    private val progressNotification by lazy { NotificationCompat.Builder(context, Notifications.CHANNEL_SYNC)
            .setContentTitle(context.getString(R.string.app_name))
            .setSmallIcon(R.drawable.ic_import_export_white_24dp)
            .setLargeIcon(notificationBitmap)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
    }
    
    override fun onPerformSync(account: Account, extras: Bundle?, authority: String?, provider: ContentProviderClient?, syncResult: SyncResult?) {
        //Hide any previous error notifications
        cancelErrorNotification()
        try {
            //Actually perform sync
            performSync(account)
        } catch(e: HandledSyncException) {
            updateSyncError(e.error)
            Timber.e(e.cause, "Sync failed! Error type: %s", e.error.name)
        } finally {
            //Hide progress as sync is done
            cancelProgressNotification()
        }
    }
    
    private fun performSync(account: Account) {
        //Auth with server (get token)
        updateSync(SyncStatus.AUTH)
        val api = TWApi.apiFromAccount(account)
        var token: String? = null
        try {
            //Three tries
            for (i in 1 .. 3) {
                token = context.accountManager.blockingGetAuthToken(account,
                        SyncAccountAuthenticator.AUTH_TOKEN_TYPE,
                        true) ?: return
                //Verify we are authenticated first
                if (api.testAuthenticated(token)
                        .toBlocking()
                        .first()
                        .success) {
                    break
                } else {
                    //Unsuccessful, get a new auth token
                    context.accountManager.invalidateAuthToken(LibrarySyncManager.ACCOUNT_TYPE,
                            token)
                    token = null
                    Timber.w("Sync authentication token is invalid, retrieving a new one!")
                }
            }
            if (token == null) {
                throw HandledSyncException(SyncError.AUTH_ERROR, null)
            }
        } catch(e: IOException) {
            throw HandledSyncException(SyncError.NETWORK_ERROR, e)
        } catch(e: Exception) {
            throw HandledSyncException(SyncError.AUTH_ERROR, e)
        }
        
        //Lock database during sync
        db.inTransaction {
            //Take empty categories snapshot if missing
            db.takeEmptyMangaCategoriesSnapshot(LibrarySyncManager.TARGET_DEVICE_ID).executeAsBlocking()
            
            //Generate library diff
            updateSync(SyncStatus.GEN_DIFF)
            val diff = try {
                //Generate diff from last sync time to current time
                reportGenerator.gen(syncManager.getDeviceId(),
                        LibrarySyncManager.TARGET_DEVICE_ID,
                        syncManager.lastSyncDateTime,
                        System.currentTimeMillis())
            } catch (e: Exception) {
                throw HandledSyncException(SyncError.DATABASE_ERROR, e)
            }
    
            //Actually upload diff
            updateSync(SyncStatus.NETWORK)
            val result = try {
                val res = api.sync(token, TWApi.PROTOCOL_VERSION, diff).toBlocking().first()
                if(res.error != null)
                    throw RuntimeException("Sync server returned error: " + res.error)

                res
            } catch (e: Exception) {
                throw HandledSyncException(SyncError.NETWORK_ERROR, e)
            }
    
            //Apply server diff
            updateSync(SyncStatus.APPLY_DIFF)
            try {
                val serverChanges = result.serverChanges!!
                
                //Apply report
                reportApplier.apply(serverChanges)
                
                //Apply timestamp correction queue
                serverChanges.tmpApply.applyQueuedTimestamps(db)
            } catch (e: Exception) {
                throw HandledSyncException(SyncError.DATABASE_ERROR, e)
            }
    
            //Take snapshots
            updateSync(SyncStatus.CLEANUP)
            try {
                syncManager.snapshots.takeSnapshots(LibrarySyncManager.TARGET_DEVICE_ID)
                db.deleteMangaCategoriesSnapshot(LibrarySyncManager.TARGET_DEVICE_ID).executeAsBlocking()
                db.takeMangaCategoriesSnapshot(LibrarySyncManager.TARGET_DEVICE_ID).executeAsBlocking()
                //Update last sync time
                syncManager.lastSyncDateTime = System.currentTimeMillis()
            } catch (e: Exception) {
                throw HandledSyncException(SyncError.DATABASE_ERROR, e)
            }
        }
    }
    
    override fun onSyncCanceled() {
        //Do not all super as that will interrupt the sync process
        //Currently sync is not a cancellable process so we do nothing
        
        //Hide progress
        cancelProgressNotification()
    }
    
    fun updateSync(status: SyncStatus) {
        showProgressNotification(status)
    }
    
    fun updateSyncError(error: SyncError) {
        showErrorNotification(error)
    }
    
    /**
     * Shows the notification describing a sync error
     *
     * @param error the sync error
     */
    private fun showErrorNotification(error: SyncError) {
        context.notificationManager.notify(Notifications.ID_SYNC_ERROR, context.notification(Notifications.CHANNEL_SYNC) {
            setSmallIcon(android.R.drawable.stat_sys_warning)
            setLargeIcon(notificationBitmap)
            setContentTitle(context.getString(R.string.sync_error, context.getString(error.message)))
            setContentText(context.getString(R.string.sync_error_details))
            priority = NotificationCompat.PRIORITY_HIGH
            setAutoCancel(true)
        })
    }

    /**
     * Cancels the error notification.
     */
    private fun cancelErrorNotification() {
        context.notificationManager.cancel(Notifications.ID_SYNC_ERROR)
    }

    /**
     * Shows the notification containing the current sync progress
     *
     * @param status the current sync status
     */
    private fun showProgressNotification(status: SyncStatus) {
        context.notificationManager.notify(Notifications.ID_SYNC_PROGRESS, progressNotification
                .setContentTitle(context.getString(R.string.sync_status_prefix, context.getString(status.status)))
                .setProgress(status.total, status.progress, false)
                .build())
    }
    
    /**
     * Cancels the progress notification.
     */
    private fun cancelProgressNotification() {
        context.notificationManager.cancel(Notifications.ID_SYNC_PROGRESS)
    }
    
    /**
     * Enum representing current sync status
     */
    enum class SyncStatus(val status: Int) {
        AUTH(R.string.sync_status_auth),
        GEN_DIFF(R.string.sync_status_gen_diff),
        NETWORK(R.string.sync_status_network),
        APPLY_DIFF(R.string.sync_status_apply_diff),
        CLEANUP(R.string.sync_status_cleanup);
        
        val progress by lazy { values().indexOf(this) }
        val total by lazy { values().size }
    }
    
    /**
     * Enum representing possible sync errors
     */
    enum class SyncError(val message: Int) {
        AUTH_ERROR(R.string.sync_error_auth),
        NETWORK_ERROR(R.string.sync_error_network),
        DATABASE_ERROR(R.string.sync_error_database)
    }
    
    class HandledSyncException(val error: SyncError, cause: Exception?) : RuntimeException(cause)
}