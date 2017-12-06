package eu.kanade.tachiyomi.data.sync.protocol.models.common

abstract class SyncEntity<out M : Any> {
    var syncId: Long = 0L

    fun getRef() = SyncRef<M>(syncId)
}