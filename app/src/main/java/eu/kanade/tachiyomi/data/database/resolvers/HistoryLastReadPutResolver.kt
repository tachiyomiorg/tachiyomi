package eu.kanade.tachiyomi.data.database.resolvers

import android.content.ContentValues
import android.support.annotation.NonNull
import com.pushtorefresh.storio.sqlite.StorIOSQLite
import com.pushtorefresh.storio.sqlite.operations.put.PutResolver
import com.pushtorefresh.storio.sqlite.operations.put.PutResult
import com.pushtorefresh.storio.sqlite.queries.UpdateQuery
import eu.kanade.tachiyomi.data.database.inTransactionReturn
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.tables.HistoryTable

class HistoryLastReadPutResolver : PutResolver<History>() {

    /**
     * Updates last_read time of chapter
     */
    override fun performPut(@NonNull db: StorIOSQLite, @NonNull history: History): PutResult = db.inTransactionReturn {
        // Create put query
        val updateQuery = mapToUpdateQuery(history)
        val contentValues = mapToContentValues(history)

        // Execute query
        val numberOfRowsUpdated = db.internal().update(updateQuery, contentValues)

        // If chapter not found in history insert into database
        if (numberOfRowsUpdated == 0) {
            db.put().`object`(history).prepare().asRxObservable().subscribe()
        }
        // Update result
        PutResult.newUpdateResult(numberOfRowsUpdated, updateQuery.table())
    }

    /**
     * Creates update query
     * @param history object
     */
    fun mapToUpdateQuery(history: History) = UpdateQuery.builder()
            .table(HistoryTable.TABLE)
            .where("${HistoryTable.COL_CHAPTER_ID} = ?")
            .whereArgs(history.chapter_id)
            .build()

    /**
     * Create content query
     * @param history object
     */
    fun mapToContentValues(history: History) = ContentValues(1).apply {
        put(HistoryTable.COL_LAST_READ, history.last_read)
    }

}
