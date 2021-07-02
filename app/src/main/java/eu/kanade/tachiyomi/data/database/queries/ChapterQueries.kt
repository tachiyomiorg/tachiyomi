package eu.kanade.tachiyomi.data.database.queries

import com.pushtorefresh.storio.sqlite.queries.Query
import com.pushtorefresh.storio.sqlite.queries.RawQuery
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaChapter
import eu.kanade.tachiyomi.data.database.resolvers.ChapterBackupPutResolver
import eu.kanade.tachiyomi.data.database.resolvers.ChapterKnownBackupPutResolver
import eu.kanade.tachiyomi.data.database.resolvers.ChapterProgressPutResolver
import eu.kanade.tachiyomi.data.database.resolvers.ChapterSourceOrderPutResolver
import eu.kanade.tachiyomi.data.database.resolvers.MangaChapterGetResolver
import eu.kanade.tachiyomi.data.database.tables.ChapterTable
import java.util.Date

interface ChapterQueries : DbProvider {

    fun getChapters(manga: Manga) = db.get()
        .listOfObjects(Chapter::class.java)
        .withQuery(
            Query.builder()
                .table(ChapterTable.TABLE)
                .where("${ChapterTable.COL_MANGA_ID} = ?")
                .whereArgs(manga.id)
                .build()
        )
        .prepare()

    fun getRecentChapters(date: Date) = db.get()
        .listOfObjects(MangaChapter::class.java)
        .withQuery(
            RawQuery.builder()
                .query(getRecentsQuery())
                .args(date.time)
                .observesTables(ChapterTable.TABLE)
                .build()
        )
        .withGetResolver(MangaChapterGetResolver.INSTANCE)
        .prepare()

    fun getChapter(id: Long) = db.get()
        .`object`(Chapter::class.java)
        .withQuery(
            Query.builder()
                .table(ChapterTable.TABLE)
                .where("${ChapterTable.COL_ID} = ?")
                .whereArgs(id)
                .build()
        )
        .prepare()

    fun getChapter(url: String) = db.get()
        .`object`(Chapter::class.java)
        .withQuery(
            Query.builder()
                .table(ChapterTable.TABLE)
                .where("${ChapterTable.COL_URL} = ?")
                .whereArgs(url)
                .build()
        )
        .prepare()

    fun getChapter(url: String, mangaId: Long) = db.get()
        .`object`(Chapter::class.java)
        .withQuery(
            Query.builder()
                .table(ChapterTable.TABLE)
                .where("${ChapterTable.COL_URL} = ? AND ${ChapterTable.COL_MANGA_ID} = ?")
                .whereArgs(url, mangaId)
                .build()
        )
        .prepare()

    fun insertChapter(chapter: Chapter) = db.put().`object`(chapter).prepare()

    fun insertChapters(chapters: List<Chapter>) = db.put().objects(chapters).prepare()

    fun deleteChapter(chapter: Chapter) = db.delete().`object`(chapter).prepare()

    fun deleteChapters(chapters: List<Chapter>) = db.delete().objects(chapters).prepare()

    fun updateChaptersBackup(chapters: List<Chapter>) = db.put()
        .objects(chapters)
        .withPutResolver(ChapterBackupPutResolver())
        .prepare()

    fun updateKnownChaptersBackup(chapters: List<Chapter>) = db.put()
        .objects(chapters)
        .withPutResolver(ChapterKnownBackupPutResolver())
        .prepare()

    fun updateChapterProgress(chapter: Chapter) = db.put()
        .`object`(chapter)
        .withPutResolver(ChapterProgressPutResolver())
        .prepare()

    fun updateChaptersProgress(chapters: List<Chapter>) = db.put()
        .objects(chapters)
        .withPutResolver(ChapterProgressPutResolver())
        .prepare()

    fun fixChaptersSourceOrder(chapters: List<Chapter>) = db.put()
        .objects(chapters)
        .withPutResolver(ChapterSourceOrderPutResolver())
        .prepare()
}
