package eu.kanade.tachiyomi.data.sync.protocol.models.common

import eu.kanade.tachiyomi.data.sync.protocol.models.SyncReport

data class SyncRef<out T : Any>(var targetId: Long) {
    fun resolve(report: SyncReport)
            = report.entities.find {
        it.syncId == targetId
    } as T
}