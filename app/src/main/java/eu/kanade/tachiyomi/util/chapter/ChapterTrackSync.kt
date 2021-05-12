package eu.kanade.tachiyomi.util.chapter

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.util.lang.launchIO

/**
 * Helper method for syncing a remote track with the local chapters, and back
 *
 * @param db the database.
 * @param chapters a list of chapters from the source.
 * @param remoteTrack the remote Track object.
 * @param service the tracker service.
 */
fun syncChaptersWithTrackServiceTwoWay(db: DatabaseHelper, chapters: List<Chapter>, remoteTrack: Track, service: TrackService) {
    val sortedChapters = chapters.sortedBy { it.chapter_number }
    sortedChapters.forEachIndexed { index, chapter ->
        if (!chapter.read) chapter.read = index < remoteTrack.last_chapter_read
    }
    db.updateChaptersProgress(sortedChapters).executeAsBlocking()

    val localLastRead = sortedChapters.indexOfFirst { !it.read }

    // update remote
    remoteTrack.last_chapter_read = localLastRead

    launchIO {
        try {
            service.update(remoteTrack)
            db.insertTrack(remoteTrack).executeAsBlocking()
        } catch (e: Throwable) {
        }
    }
}
