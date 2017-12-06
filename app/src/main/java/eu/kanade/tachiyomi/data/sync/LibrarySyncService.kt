package eu.kanade.tachiyomi.data.sync

import android.app.Service
import android.content.Intent

/**
 * Define a Service that returns an IBinder for the
 * sync adapter class, allowing the sync adapter framework to call
 * onPerformSync().
 */
class LibrarySyncService : Service() {
    /*
     * Instantiate the sync adapter object.
     */
    override fun onCreate() {
        /*
         * Create the sync adapter as a singleton.
         * Set the sync adapter as syncable
         * Disallow parallel syncs
         */
        synchronized(sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = LibrarySyncAdapter(applicationContext)
            }
        }
    }
    
    /**
     * Return an object that allows the system to invoke
     * the sync adapter.
     */
    override fun onBind(intent: Intent) = sSyncAdapter!!.syncAdapterBinder
    
    companion object {
        // Storage for an instance of the sync adapter
        private var sSyncAdapter: LibrarySyncAdapter? = null
        // Object to use as a thread-safe lock
        private val sSyncAdapterLock = Any()
    }
}
