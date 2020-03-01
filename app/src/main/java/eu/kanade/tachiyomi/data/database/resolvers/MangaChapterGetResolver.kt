package eu.kanade.tachiyomi.data.database.resolvers

import android.database.Cursor
import com.pushtorefresh.storio.sqlite.operations.get.DefaultGetResolver
import eu.kanade.tachiyomi.data.database.mappers.ChapterGetResolver
import eu.kanade.tachiyomi.data.database.mappers.MangaGetResolver
import eu.kanade.tachiyomi.data.database.models.MangaChapter

class MangaChapterGetResolver : DefaultGetResolver<MangaChapter>() {

    companion object {
        val INSTANCE = MangaChapterGetResolver()
    }

    private val mangaGetResolver = MangaGetResolver()

    private val chapterGetResolver = ChapterGetResolver()

    override fun mapFromCursor(cursor: Cursor): MangaChapter {
        val manga = mangaGetResolver.mapFromCursor(cursor)
        val chapter = chapterGetResolver.mapFromCursor(cursor)
        manga.id = chapter.manga_id
        manga.url = cursor.getString(cursor.getColumnIndex("mangaUrl"))

        return MangaChapter(manga, chapter)
    }
}
