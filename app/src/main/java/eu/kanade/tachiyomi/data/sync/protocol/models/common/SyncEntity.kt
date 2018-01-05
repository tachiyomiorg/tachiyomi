package eu.kanade.tachiyomi.data.sync.protocol.models.common

/**
 * An entity
 *
 * @param M The type of the subclass
 */
abstract class SyncEntity<out M : Any> {
    /**
     * A unique ID identifying this object in a sync report
     * No two entities in the same sync report can share the same ID
     */
    var syncId: Long = 0L
    
    /**
     * Get a reference to this entity using it's sync ID
     */
    fun getRef() = SyncRef<M>(syncId)
}