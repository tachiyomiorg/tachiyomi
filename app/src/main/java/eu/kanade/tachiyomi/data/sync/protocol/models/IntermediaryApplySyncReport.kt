package eu.kanade.tachiyomi.data.sync.protocol.models

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.EntryUpdate
import eu.kanade.tachiyomi.data.database.models.UpdatableField
import eu.kanade.tachiyomi.data.sync.protocol.models.common.SyncEntity

/**
 * Intermediary data structure with various optimizations used to apply sync report
 */
class IntermediaryApplySyncReport(val report: SyncReport) {
    var setup = false
    
    private var lastQueuedId = 0L
    
    lateinit var sortedEntities: List<SyncEntity<*>>
    var queuedTimestampEntries = mutableListOf<QueuedTimestampEntry>()
    var queuedInsertedIds = mutableListOf<QueuedInsertedId>()
    
    fun nextQueuedId() = --lastQueuedId
    
    fun setup() {
        sortedEntities = report.entities.sortedBy { it.syncId }
        
        setup = true
    }
    
    fun applyQueuedTimestamps(db: DatabaseHelper) {
        //Prepare queued inserted IDs for binary search
        queuedInsertedIds.sortBy { it.oldId }
        
        queuedTimestampEntries.forEach {
            val id = if(it.id < 0) {
                //Find inserted id if entity does not have a valid id
                queuedInsertedIds[queuedInsertedIds.binarySearchBy(it.id, selector = QueuedInsertedId::oldId).let {
                    if (it < 0)
                        return@forEach //Never inserted, ignore this entity
                    else it
                }].dbId
            } else it.id //Entity has valid id, use it
            
            val newEntryUpdate = EntryUpdate.create(id, it.time, it.field)
            
            //Correct timestamp
            db.replaceEntryUpdate(newEntryUpdate).executeAsBlocking()
        }
    }
    
    data class QueuedTimestampEntry(val id: Long,
                                    val field: UpdatableField,
                                    val time: Long)
    
    data class QueuedInsertedId(val oldId: Long,
                                val dbId: Long)
}
