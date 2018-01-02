package eu.kanade.tachiyomi.data.sync.protocol.models.entities

import eu.kanade.tachiyomi.data.sync.protocol.models.common.ChangedField
import eu.kanade.tachiyomi.data.sync.protocol.models.common.SyncEntity
import eu.kanade.tachiyomi.data.sync.protocol.models.common.SyncRef

class SyncHistory : SyncEntity<SyncHistory>() {
    // Changes
    var lastRead: ChangedField<Long>? = null

    // Identifiers
    lateinit var chapter: SyncRef<SyncChapter>
}