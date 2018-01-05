package eu.kanade.tachiyomi.data.sync.protocol.models.common

import eu.kanade.tachiyomi.data.sync.protocol.models.SyncReport

/**
 * A sync response from the server
 */
class SyncResponse {
    /**
     * The error that occurred on the server when running the sync operation
     * `null` if the sync was successful and no error occurred
     */
    var error: String? = null
    
    /**
     * The server's sync report
     */
    var serverChanges: SyncReport? = null
}