package eu.kanade.tachiyomi.data.database.resolvers

import androidx.core.content.contentValuesOf
import com.pushtorefresh.storio.sqlite.StorIOSQLite
import com.pushtorefresh.storio.sqlite.operations.put.PutResolver
import com.pushtorefresh.storio.sqlite.operations.put.PutResult
import com.pushtorefresh.storio.sqlite.queries.UpdateQuery
import eu.kanade.tachiyomi.data.database.inTransactionReturn
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.tables.ChapterTable

class ChapterProgressPutResolver : PutResolver<Chapter>() {

    override fun performPut(db: StorIOSQLite, chapter: Chapter) = db.inTransactionReturn {
        val updateQuery = mapToUpdateQuery(chapter)
        val contentValues = mapToContentValues(chapter)

        val numberOfRowsUpdated = db.lowLevel().update(updateQuery, contentValues)
        PutResult.newUpdateResult(numberOfRowsUpdated, updateQuery.table())
    }

    fun mapToUpdateQuery(chapter: Chapter) = UpdateQuery.builder()
        .table(ChapterTable.TABLE)
        .where("${ChapterTable.COL_ID} = ?")
        .whereArgs(chapter.id)
        .build()

    fun mapToContentValues(chapter: Chapter) =
        contentValuesOf(
            ChapterTable.COL_READ to chapter.read,
            ChapterTable.COL_BOOKMARK to chapter.bookmark,
            ChapterTable.COL_LAST_PAGE_READ to chapter.last_page_read
        )
}
