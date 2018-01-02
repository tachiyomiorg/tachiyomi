package eu.kanade.tachiyomi.data.sync.protocol.models.entities

import eu.kanade.tachiyomi.data.sync.protocol.models.common.ChangedField
import eu.kanade.tachiyomi.data.sync.protocol.models.common.SyncEntity
import eu.kanade.tachiyomi.data.sync.protocol.models.common.SyncRef

class SyncManga : SyncEntity<SyncManga>() {
    // Changes
    var favorite: ChangedField<Boolean>? = null
    var viewer: ChangedField<Int>? = null
    var chapterFlags: ChangedField<Int>? = null

    // Identifiers
    lateinit var source: SyncRef<SyncSource>
    lateinit var url: String
    lateinit var name: String
    var thumbnailUrl: String? = null
}