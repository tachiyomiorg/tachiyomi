package eu.kanade.tachiyomi.data.sync.protocol.snapshot

import eu.kanade.tachiyomi.data.database.models.Track

/**
 * A snapshot of a [Track]
 */
data class TrackSnapshot(val mangaId: Long,
                         val syncId: Int) {
    
    constructor(track: Track): this(track.manga_id, track.sync_id)
    
    fun serialize() = "$mangaId:$syncId"
    
    fun matches(track: Track) = track.manga_id == mangaId && track.sync_id == syncId
    
    companion object {
        fun deserialize(string: String)
                = TrackSnapshot(
                string.substringBefore(':').toLong(),
                string.substringAfter(':').toInt()
        )
    }
}
