package eu.kanade.tachiyomi.data.sync.protocol.models

import eu.kanade.tachiyomi.data.sync.protocol.models.common.ChangedField
import eu.kanade.tachiyomi.data.sync.protocol.models.common.SyncEntity
import eu.kanade.tachiyomi.data.sync.protocol.models.common.SyncRef

class SyncCategory : SyncEntity<SyncCategory>() {
    // Changes
    var addedManga: MutableList<SyncRef<SyncManga>> = mutableListOf()
    var deletedManga: MutableList<SyncRef<SyncManga>> = mutableListOf()
    var flags: ChangedField<Int>? = null
    var oldName: ChangedField<String>? = null
    var deleted: Boolean = false

    // Identifiers
    lateinit var name: String
}