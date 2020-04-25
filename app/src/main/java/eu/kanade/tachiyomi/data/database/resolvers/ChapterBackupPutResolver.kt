package eu.kanade.tachiyomi.data.database.resolvers

import android.content.ContentValues
import com.pushtorefresh.storio.sqlite.StorIOSQLite
import com.pushtorefresh.storio.sqlite.operations.put.PutResolver
import com.pushtorefresh.storio.sqlite.operations.put.PutResult
import com.pushtorefresh.storio.sqlite.queries.UpdateQuery
import eu.kanade.tachiyomi.data.database.inTransactionReturn
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.tables.ChapterTable

class ChapterBackupPutResolver : PutResolver<Chapter>() {

    override fun performPut(db: StorIOSQLite, chapter: Chapter) = db.inTransactionReturn {
        val updateQuery = mapToUpdateQuery(chapter)
        val contentValues = mapToContentValues(chapter)

        val numberOfRowsUpdated = db.lowLevel().update(updateQuery, contentValues)
        PutResult.newUpdateResult(numberOfRowsUpdated, updateQuery.table())
    }

    fun mapToUpdateQuery(chapter: Chapter) = UpdateQuery.builder()
        .table(ChapterTable.TABLE)
        .where("${ChapterTable.COL_URL} = ?")
        .whereArgs(chapter.url)
        .build()

    fun mapToContentValues(chapter: Chapter) = ContentValues(3).apply {
        put(ChapterTable.COL_READ, chapter.read)
        put(ChapterTable.COL_BOOKMARK, chapter.bookmark)
        put(ChapterTable.COL_LAST_PAGE_READ, chapter.last_page_read)
    }
}
