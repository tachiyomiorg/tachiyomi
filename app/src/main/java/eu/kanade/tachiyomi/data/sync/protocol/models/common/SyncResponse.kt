package eu.kanade.tachiyomi.data.sync.protocol.models.common

import eu.kanade.tachiyomi.data.sync.protocol.models.SyncReport

class SyncResponse {
    var error: String? = null
    var serverChanges: SyncReport? = null
}