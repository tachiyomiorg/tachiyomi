package eu.kanade.tachiyomi.data.sync

import android.content.Context
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import uy.kohesive.injekt.injectLazy
import java.util.*

class LibrarySyncManager(val context: Context) {
    private val prefs: PreferencesHelper by injectLazy()
    
    fun getDeviceId()
        = prefs.syncId().get() ?: run {
        val deviceId = UUID.randomUUID().toString().replace('-', '_')
        prefs.syncId().set(deviceId)
        deviceId
    }
    
    fun getLastSync() = prefs.lastSync().get()!!
    fun setLastSync(lastSync: Long) = prefs.lastSync().set(lastSync)
    
    companion object {
        val TARGET_DEVICE_ID = "server"
    }
}
