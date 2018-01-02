package eu.kanade.tachiyomi.data.sync.protocol.models.entities

import eu.kanade.tachiyomi.data.sync.protocol.models.common.SyncEntity

class SyncSource : SyncEntity<SyncSource>() {
    // Identifiers
    var id: Long = -1
    lateinit var name: String
}