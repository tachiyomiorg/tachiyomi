package eu.kanade.tachiyomi.data.database.resolvers

import android.database.Cursor
import com.pushtorefresh.storio.sqlite.operations.get.DefaultGetResolver
import eu.kanade.tachiyomi.data.database.models.*

class MangaChapterHistoryGetResolver : DefaultGetResolver<MangaChapterHistory>() {
    companion object {
        val INSTANCE = MangaChapterHistoryGetResolver()
    }

    /**
     * Manga get resolver
     */
    private val mangaGetResolver = MangaStorIOSQLiteGetResolver()

    /**
     * Chapter get resolver
     */
    private val chapterResolver = ChapterStorIOSQLiteGetResolver()

    /**
     * History get resolver
     */
    private val historyGetResolver = HistoryStorIOSQLiteGetResolver()

    /**
     * Map correct objects from cursor result
     */
    override fun mapFromCursor(cursor: Cursor): MangaChapterHistory {
        // Get manga object
        val manga = mangaGetResolver.mapFromCursor(cursor)

        // Get chapter object
        val chapter = chapterResolver.mapFromCursor(cursor)

        // Get history object
        val history = historyGetResolver.mapFromCursor(cursor)

        // Make certain column conflicts are dealt with
        manga.id = chapter.manga_id
        manga.url = cursor.getString(cursor.getColumnIndex("mangaUrl"))
        chapter.id = history.chapter_id

        // Create mangaChapter object
        val mangaChapter = MangaChapter(manga, chapter)

        // Return result
        return MangaChapterHistory(mangaChapter, history)
    }
}
