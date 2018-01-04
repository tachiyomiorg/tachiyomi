package eu.kanade.tachiyomi.data.sync.protocol.models

import eu.kanade.tachiyomi.data.sync.protocol.models.common.SyncEntity

/**
 * Intermediary data structure with various optimizations used to apply sync report
 */
class IntermediaryApplySyncReport(val report: SyncReport) {
    var setup = false
    
    lateinit var sortedEntities: List<SyncEntity<*>>
    
    fun setup() {
        sortedEntities = report.entities.sortedBy { it.syncId }
        
        setup = true
    }
}
