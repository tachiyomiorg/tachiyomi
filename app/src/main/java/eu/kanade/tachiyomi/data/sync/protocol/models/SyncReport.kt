package eu.kanade.tachiyomi.data.sync.protocol.models

import eu.kanade.tachiyomi.data.sync.protocol.models.common.SyncEntity

/**
 * A sync report
 */
class SyncReport {
    // Temporary data structures used when generating/applying the report
    
    /**
     * The last ID used in this sync report
     *
     * Used when building the sync report
     */
    @Transient
    var lastId = 0L
    
    /**
     * A temporary data structure used to build this sync report
     */
    @Transient
    lateinit var tmpGen: IntermediaryGenSyncReport
    
    /**
     * A temporary data structure used to apply this sync report
     */
    @Transient
    lateinit var tmpApply: IntermediaryApplySyncReport
    
    // Actual sync report follows
    
    /**
     * The device ID of the device generating this sync report
     *
     * Usually a UUID with it's dashes replaced with underscores
     */
    var deviceId: String = ""
    
    /**
     * The entities in this sync report
     */
    var entities: MutableList<SyncEntity<*>> = mutableListOf()
    
    /**
     * The earliest date the data in this sync report covers
     *
     * In millis since epoch in UTC
     */
    var from: Long = -1
    
    /**
     * The latest date the data in this sync report covers
     *
     * In millis since epoch in UTC
     */
    var to: Long = -1
    
    /**
     * Find all entities in this report that are of the type [M]
     */
    inline fun <reified M : SyncEntity<*>> findEntities()
        = entities.asSequence().filterIsInstance<M>()
    
    /**
     * Find all entities in this report that are of the type [M]
     * and match the provided [filter]
     */
    inline fun <reified M : SyncEntity<*>> findEntity(filter: (M) -> Boolean): M?
        = findEntities<M>().find(filter)

    fun isTmpApplySetup() = ::tmpApply.isInitialized
}