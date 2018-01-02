package eu.kanade.tachiyomi.data.sync

import android.content.Context
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import uy.kohesive.injekt.injectLazy
import java.util.*

class LibrarySyncManager(val context: Context) {
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
    
    companion object {
        val TARGET_DEVICE_ID = "server"
    }
}
