package eu.kanade.tachiyomi.data.sync.protocol.models.common

import eu.kanade.tachiyomi.data.sync.protocol.models.SyncReport

/**
 * A reference to a [SyncEntity]
 */
data class SyncRef<out T : Any>(var targetId: Long) {
    /**
     * Resolve this reference in a [report]
     * Always assumes the referred entity exists inside
     * the specified [report]
     *
     * @param report The report to find the referred [SyncEntity] in
     */
    fun resolve(report: SyncReport): T {
        val result = if(report.isTmpApplySetup()) {
            //Use binary search if intermediary data structure is set up
            val index = report.tmpApply.sortedEntities.binarySearchBy(targetId,
                    selector = SyncEntity<*>::syncId)
            report.tmpApply.sortedEntities[index]
        } else report.entities.find {
            it.syncId == targetId
        }
        
        return result as T
    }
}