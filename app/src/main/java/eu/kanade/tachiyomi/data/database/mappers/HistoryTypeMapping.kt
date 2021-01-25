package eu.kanade.tachiyomi.data.database.mappers

import android.database.Cursor
import androidx.core.content.contentValuesOf
import com.pushtorefresh.storio.sqlite.SQLiteTypeMapping
import com.pushtorefresh.storio.sqlite.operations.delete.DefaultDeleteResolver
import com.pushtorefresh.storio.sqlite.operations.get.DefaultGetResolver
import com.pushtorefresh.storio.sqlite.operations.put.DefaultPutResolver
import com.pushtorefresh.storio.sqlite.queries.DeleteQuery
import com.pushtorefresh.storio.sqlite.queries.InsertQuery
import com.pushtorefresh.storio.sqlite.queries.UpdateQuery
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.models.HistoryImpl
import eu.kanade.tachiyomi.data.database.tables.HistoryTable.COL_CHAPTER_ID
import eu.kanade.tachiyomi.data.database.tables.HistoryTable.COL_ID
import eu.kanade.tachiyomi.data.database.tables.HistoryTable.COL_LAST_READ
import eu.kanade.tachiyomi.data.database.tables.HistoryTable.COL_TIME_READ
import eu.kanade.tachiyomi.data.database.tables.HistoryTable.TABLE

class HistoryTypeMapping : SQLiteTypeMapping<History>(
    HistoryPutResolver(),
    HistoryGetResolver(),
    HistoryDeleteResolver()
)

open class HistoryPutResolver : DefaultPutResolver<History>() {

    override fun mapToInsertQuery(obj: History) = InsertQuery.builder()
        .table(TABLE)
        .build()

    override fun mapToUpdateQuery(obj: History) = UpdateQuery.builder()
        .table(TABLE)
        .where("$COL_ID = ?")
        .whereArgs(obj.id)
        .build()

    override fun mapToContentValues(obj: History) =
        contentValuesOf(
            COL_ID to obj.id,
            COL_CHAPTER_ID to obj.chapter_id,
            COL_LAST_READ to obj.last_read,
            COL_TIME_READ to obj.time_read
        )
}

class HistoryGetResolver : DefaultGetResolver<History>() {

    override fun mapFromCursor(cursor: Cursor): History = HistoryImpl().apply {
        id = cursor.getLong(cursor.getColumnIndex(COL_ID))
        chapter_id = cursor.getLong(cursor.getColumnIndex(COL_CHAPTER_ID))
        last_read = cursor.getLong(cursor.getColumnIndex(COL_LAST_READ))
        time_read = cursor.getLong(cursor.getColumnIndex(COL_TIME_READ))
    }
}

class HistoryDeleteResolver : DefaultDeleteResolver<History>() {

    override fun mapToDeleteQuery(obj: History) = DeleteQuery.builder()
        .table(TABLE)
        .where("$COL_ID = ?")
        .whereArgs(obj.id)
        .build()
}
