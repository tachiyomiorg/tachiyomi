package xyz.nulldev.ts.sync.model

/**
 * A basic sync request
 */
data class SyncRequest(val old_library: String,
                       val new_library: String,
                       val favorites_only: Boolean) {}