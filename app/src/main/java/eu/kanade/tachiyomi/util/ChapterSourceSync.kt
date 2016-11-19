package eu.kanade.tachiyomi.util

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.source.Source
import eu.kanade.tachiyomi.data.source.online.OnlineSource
import java.util.*

/**
 * Helper method for syncing the list of chapters from the source with the ones from the database.
 *
 * @param db the database.
 * @param sourceChapters a list of chapters from the source.
 * @param manga the manga of the chapters.
 * @param source the source of the chapters.
 * @return a pair of new insertions and deletions.
 */
fun syncChaptersWithSource(db: DatabaseHelper,
                           sourceChapters: List<Chapter>,
                           manga: Manga,
                           source: Source) : Pair<List<Chapter>, List<Chapter>> {

    // Chapters from db.
    val dbChapters = db.getChapters(manga).executeAsBlocking()

    // Fix manga id and order in source.
    sourceChapters.forEachIndexed { i, chapter ->
        chapter.manga_id = manga.id
        chapter.source_order = i
    }

    // Chapters from the source not in db.
    val toAdd = sourceChapters.filterNot { it in dbChapters }

    // Recognize number for new chapters.
    toAdd.forEach {
        if (source is OnlineSource) {
            source.prepareNewChapter(it, manga)
        }
        ChapterRecognition.parseChapterNumber(it, manga)
    }

    // Chapters from the db not in the source.
    val toDelete = dbChapters.filterNot { it in sourceChapters }

    val readded = mutableListOf<Chapter>()

    db.inTransaction {
        val deletedChapterNumbers = TreeSet<Float>()
        val deletedReadChapterNumbers = TreeSet<Float>()
        if (!toDelete.isEmpty()) {
            for (c in toDelete) {
                if (c.read) {
                    deletedReadChapterNumbers.add(c.chapter_number)
                }
                deletedChapterNumbers.add(c.chapter_number)
            }
            db.deleteChapters(toDelete).executeAsBlocking()
        }

        if (!toAdd.isEmpty()) {
            // Set the date fetch for new items in reverse order to allow another sorting method.
            // Sources MUST return the chapters from most to less recent, which is common.
            var now = Date().time

            for (i in toAdd.indices.reversed()) {
                val c = toAdd[i]
                c.date_fetch = now++
                // Try to mark already read chapters as read when the source deletes them
                if (c.isRecognizedNumber && c.chapter_number in deletedReadChapterNumbers) {
                    c.read = true
                }
                if (c.isRecognizedNumber && c.chapter_number in deletedChapterNumbers) {
                    readded.add(c)
                }
            }
            db.insertChapters(toAdd).executeAsBlocking()
        }

        // Fix order in source.
        db.fixChaptersSourceOrder(sourceChapters).executeAsBlocking()
    }
    return Pair(toAdd.subtract(readded).toList(), toDelete.subtract(readded).toList())
}
