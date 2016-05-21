package eu.kanade.tachiyomi.data.database.resolvers

import android.database.Cursor
import com.pushtorefresh.storio.sqlite.operations.get.DefaultGetResolver
import eu.kanade.tachiyomi.data.database.models.ChapterStorIOSQLiteGetResolver
import eu.kanade.tachiyomi.data.database.models.MangaChapter
import eu.kanade.tachiyomi.data.database.models.MangaStorIOSQLiteGetResolver

class MangaChapterGetResolver : DefaultGetResolver<MangaChapter>() {

    companion object {
        val INSTANCE = MangaChapterGetResolver()
    }

    private val mangaGetResolver = MangaStorIOSQLiteGetResolver()

    private val chapterGetResolver = ChapterStorIOSQLiteGetResolver()

    override fun mapFromCursor(cursor: Cursor): MangaChapter {
        val manga = mangaGetResolver.mapFromCursor(cursor)
        val chapter = chapterGetResolver.mapFromCursor(cursor)
        manga.id = chapter.manga_id
        manga.url = cursor.getString(cursor.getColumnIndex("mangaUrl"))

        return MangaChapter(manga, chapter)
    }

}
