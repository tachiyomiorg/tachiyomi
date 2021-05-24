package eu.kanade.tachiyomi.util.chapter

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.util.lang.launchIO
import timber.log.Timber

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
    sortedChapters
        .filterIndexed { index, chapter -> index < remoteTrack.last_chapter_read && !chapter.read }
        .forEach { it.read = true }
    db.updateChaptersProgress(sortedChapters).executeAsBlocking()

    val localLastRead = when {
        sortedChapters.all { it.read } -> sortedChapters.size
        sortedChapters.any { !it.read } -> sortedChapters.indexOfFirst { !it.read }
        else -> 0
    }

    // update remote
    remoteTrack.last_chapter_read = localLastRead

    launchIO {
        try {
            service.update(remoteTrack)
            db.insertTrack(remoteTrack).executeAsBlocking()
        } catch (e: Throwable) {
            Timber.w(e)
        }
    }
}
