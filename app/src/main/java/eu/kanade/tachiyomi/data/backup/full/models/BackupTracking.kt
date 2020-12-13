package eu.kanade.tachiyomi.data.backup.full.models

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.database.models.TrackImpl
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class BackupTracking(
    // in 1.x some of these values have different types or names
    // syncId is called siteId in 1,x
    @ProtoNumber(1) var syncId: Int,
    // LibraryId is not null in 1.x
    @ProtoNumber(2) var libraryId: Long,
    @ProtoNumber(3) var mediaId: Int = 0,
    // trackingUrl is called mediaUrl in 1.x
    @ProtoNumber(4) var trackingUrl: String = "",
    @ProtoNumber(5) var title: String = "",
    // lastChapterRead is called last read, and it has been changed to a float in 1.x
    @ProtoNumber(6) var lastChapterRead: Float = 0F,
    @ProtoNumber(7) var totalChapters: Int = 0,
    @ProtoNumber(8) var score: Float = 0F,
    @ProtoNumber(9) var status: Int = 0,
    // startedReadingDate is called startReadTime in 1.x
    @ProtoNumber(10) var startedReadingDate: Long = 0,
    // finishedReadingDate is called endReadTime in 1.x
    @ProtoNumber(11) var finishedReadingDate: Long = 0,
) {
    fun getTrackingImpl(): TrackImpl {
        return TrackImpl().apply {
            sync_id = this@BackupTracking.syncId
            media_id = this@BackupTracking.mediaId
            library_id = this@BackupTracking.libraryId
            title = this@BackupTracking.title
            // convert from float to int because of 1.x types
            last_chapter_read = this@BackupTracking.lastChapterRead.toInt()
            total_chapters = this@BackupTracking.totalChapters
            score = this@BackupTracking.score
            status = this@BackupTracking.status
            started_reading_date = this@BackupTracking.startedReadingDate
            finished_reading_date = this@BackupTracking.finishedReadingDate
            tracking_url = this@BackupTracking.trackingUrl
        }
    }

    companion object {
        fun copyFrom(track: Track): BackupTracking {
            return BackupTracking(
                syncId = track.sync_id,
                mediaId = track.media_id,
                // forced not null so its compatible with 1.x backup system
                libraryId = track.library_id!!,
                title = track.title,
                // convert to float for 1.x
                lastChapterRead = track.last_chapter_read.toFloat(),
                totalChapters = track.total_chapters,
                score = track.score,
                status = track.status,
                startedReadingDate = track.started_reading_date,
                finishedReadingDate = track.finished_reading_date,
                trackingUrl = track.tracking_url
            )
        }
    }
}
