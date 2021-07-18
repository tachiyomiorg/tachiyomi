package eu.kanade.tachiyomi.util.chapter

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.online.HttpSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Date
import java.util.TreeSet

/**
 * Helper method for syncing the list of chapters from the source with the ones from the database.
 *
 * @param db the database.
 * @param rawSourceChapters a list of chapters from the source.
 * @param manga the manga of the chapters.
 * @param source the source of the chapters.
 * @return a pair of new insertions and deletions.
 */
fun syncChaptersWithSource(
    db: DatabaseHelper,
    rawSourceChapters: List<SChapter>,
    manga: Manga,
    source: Source
): Pair<List<Chapter>, List<Chapter>> {
    if (rawSourceChapters.isEmpty()) {
        throw NoChaptersException()
    }

    val downloadManager: DownloadManager = Injekt.get()

    // Chapters from db.
    val dbChapters = db.getChapters(manga).executeAsBlocking()

    val sourceChapters = rawSourceChapters
        .distinctBy { it.url }
        .mapIndexed { i, sChapter ->
            Chapter.create().apply {
                copyFrom(sChapter)
                manga_id = manga.id
                source_order = i
            }
        }

    // Chapters from the source not in db.
    val toAdd = mutableListOf<Chapter>()

    // Chapters whose metadata have changed.
    val toChange = mutableListOf<Chapter>()

    for (sourceChapter in sourceChapters) {
        val dbChapter = dbChapters.find { it.url == sourceChapter.url }

        // Add the chapter if not in db already, or update if the metadata changed.
        if (dbChapter == null) {
            toAdd.add(sourceChapter)
        } else {
            // this forces metadata update for the main viewable things in the chapter list
            if (source is HttpSource) {
                source.prepareNewChapter(sourceChapter, manga)
            }

            ChapterRecognition.parseChapterNumber(sourceChapter, manga)

            if (shouldUpdateDbChapter(dbChapter, sourceChapter)) {
                if (dbChapter.name != sourceChapter.name && downloadManager.isChapterDownloaded(dbChapter, manga)) {
                    downloadManager.renameChapter(source, manga, dbChapter, sourceChapter)
                }
                dbChapter.scanlator = sourceChapter.scanlator
                dbChapter.name = sourceChapter.name
                dbChapter.date_upload = sourceChapter.date_upload
                dbChapter.chapter_number = sourceChapter.chapter_number
                dbChapter.source_order = sourceChapter.source_order
                toChange.add(dbChapter)
            }
        }
    }

    // Recognize number for new chapters.
    toAdd.forEach {
        if (source is HttpSource) {
            source.prepareNewChapter(it, manga)
        }
        ChapterRecognition.parseChapterNumber(it, manga)
    }

    // Chapters from the db not in the source.
    val toDelete = dbChapters.filterNot { dbChapter ->
        sourceChapters.any { sourceChapter ->
            dbChapter.url == sourceChapter.url
        }
    }

    // Return if there's nothing to add, delete or change, avoiding unnecessary db transactions.
    if (toAdd.isEmpty() && toDelete.isEmpty() && toChange.isEmpty()) {
        val topChapters = dbChapters.sortedByDescending { it.date_upload }.take(4)
        val newestDate = topChapters.getOrNull(0)?.date_upload ?: 0L

        // Recalculate update rate if unset and enough chapters are present
        if (manga.next_update == 0L && topChapters.size > 1) {
            var delta = 0L
            for (i in 0 until topChapters.size - 1) {
                delta += (topChapters[i].date_upload - topChapters[i + 1].date_upload)
            }
            delta /= topChapters.size - 1
            manga.next_update = newestDate + delta
            db.updateNextUpdated(manga).executeAsBlocking()
        }

        if (newestDate != 0L && newestDate != manga.last_update) {
            manga.last_update = newestDate
            db.updateLastUpdated(manga).executeAsBlocking()
        }
        return Pair(emptyList(), emptyList())
    }

    val readded = mutableListOf<Chapter>()

    db.inTransaction {
        val deletedChapterNumbers = TreeSet<Float>()
        val deletedReadChapterNumbers = TreeSet<Float>()
        if (toDelete.isNotEmpty()) {
            for (c in toDelete) {
                if (c.read) {
                    deletedReadChapterNumbers.add(c.chapter_number)
                }
                deletedChapterNumbers.add(c.chapter_number)
            }
            db.deleteChapters(toDelete).executeAsBlocking()
        }

        if (toAdd.isNotEmpty()) {
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
            val chapters = db.insertChapters(toAdd).executeAsBlocking()
            toAdd.forEach { chapter ->
                chapter.id = chapters.results().getValue(chapter).insertedId()
            }
        }

        if (toChange.isNotEmpty()) {
            db.insertChapters(toChange).executeAsBlocking()
        }

        val topChapters = db.getChapters(manga).executeAsBlocking()
            .sortedByDescending { it.date_upload }
            .take(4)
        // Recalculate next update since chapters were changed
        if (topChapters.size > 1) {
            var delta = 0L
            for (i in 0 until topChapters.size - 1) {
                delta += (topChapters[i].date_upload - topChapters[i + 1].date_upload)
            }
            delta /= topChapters.size - 1
            manga.next_update = topChapters[0].date_upload + delta
            db.updateNextUpdated(manga).executeAsBlocking()
        }

        // Fix order in source.
        db.fixChaptersSourceOrder(sourceChapters).executeAsBlocking()

        // Set this manga as updated since chapters were changed
        val newestChapter = topChapters.getOrNull(0)
        val dateFetch = newestChapter?.date_upload ?: manga.last_update
        if (dateFetch == 0L) {
            if (toAdd.isNotEmpty()) {
                manga.last_update = Date().time
            }
        } else manga.last_update = dateFetch
        db.updateLastUpdated(manga).executeAsBlocking()
    }

    return Pair(toAdd.subtract(readded).toList(), toDelete.subtract(readded).toList())
}

private fun shouldUpdateDbChapter(dbChapter: Chapter, sourceChapter: Chapter): Boolean {
    return dbChapter.scanlator != sourceChapter.scanlator || dbChapter.name != sourceChapter.name ||
        dbChapter.date_upload != sourceChapter.date_upload ||
        dbChapter.chapter_number != sourceChapter.chapter_number ||
        dbChapter.source_order != sourceChapter.source_order
}

class NoChaptersException : Exception()
