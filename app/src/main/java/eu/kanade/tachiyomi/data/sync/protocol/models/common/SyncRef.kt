package eu.kanade.tachiyomi.data.sync.protocol.models.common

import eu.kanade.tachiyomi.data.sync.protocol.models.SyncReport

data class SyncRef<out T : Any>(var targetId: Long) {
    fun resolve(report: SyncReport): T {
        val result = if(report.tmpApply.setup) {
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