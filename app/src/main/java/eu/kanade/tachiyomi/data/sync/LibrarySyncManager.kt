package eu.kanade.tachiyomi.data.sync

import android.content.ContentResolver
import android.content.Context
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.sync.protocol.snapshot.SnapshotHelper
import eu.kanade.tachiyomi.util.accountManager
import uy.kohesive.injekt.injectLazy
import java.util.*

class LibrarySyncManager(private val context: Context) {
    private val prefs: PreferencesHelper by injectLazy()
    
    fun getDeviceId()
        = prefs.syncId().get() ?: regenDeviceId()
    
    fun regenDeviceId(): String {
        val deviceId = UUID.randomUUID().toString().replace('-', '_')
        prefs.syncId().set(deviceId)
        return deviceId
    }
    
    var lastSyncDateTime
        get() = prefs.lastSync().get()!!
        set(v) { prefs.lastSync().set(v) }
    
    val account
        get() = context.accountManager.getAccountsByType(ACCOUNT_TYPE).firstOrNull()
    
    val isSyncActive: Boolean
        get() = ContentResolver.getCurrentSyncs().any {
            it.authority == LibrarySyncManager.CONTENT_PROVIDER
        }
    
    val snapshots by lazy { SnapshotHelper(context) }
    
    companion object {
        val TARGET_DEVICE_ID = "server"
        val ACCOUNT_TYPE = "${BuildConfig.APPLICATION_ID}.sync-account"
        val CONTENT_PROVIDER = "${BuildConfig.APPLICATION_ID}.sync-provider"
    }
}
