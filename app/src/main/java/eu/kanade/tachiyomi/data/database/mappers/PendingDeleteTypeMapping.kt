package eu.kanade.tachiyomi.data.database.mappers

import android.content.ContentValues
import android.database.Cursor
import com.pushtorefresh.storio.sqlite.SQLiteTypeMapping
import com.pushtorefresh.storio.sqlite.operations.delete.DefaultDeleteResolver
import com.pushtorefresh.storio.sqlite.operations.get.DefaultGetResolver
import com.pushtorefresh.storio.sqlite.operations.put.DefaultPutResolver
import com.pushtorefresh.storio.sqlite.queries.DeleteQuery
import com.pushtorefresh.storio.sqlite.queries.InsertQuery
import com.pushtorefresh.storio.sqlite.queries.UpdateQuery
import eu.kanade.tachiyomi.data.database.tables.PendingDeleteTable.TABLE
import eu.kanade.tachiyomi.data.database.tables.PendingDeleteTable.COL_ID
import eu.kanade.tachiyomi.data.database.tables.PendingDeleteTable.COL_CHAPTER_ID
import eu.kanade.tachiyomi.data.database.models.PendingDeleteItem
import eu.kanade.tachiyomi.data.database.models.PendingDeleteItemImpl

class PendingDeleteTypeMapping : SQLiteTypeMapping<PendingDeleteItem>(
        PendingDeletePutResolver(),
        PendingDeleteGetResolver(),
        PendingDeleteDeleteResolver()
)

class PendingDeletePutResolver : DefaultPutResolver<PendingDeleteItem>() {

    override fun mapToInsertQuery(obj: PendingDeleteItem) = InsertQuery.builder()
            .table(TABLE)
            .build()

    override fun mapToUpdateQuery(obj: PendingDeleteItem) = UpdateQuery.builder()
            .table(TABLE)
            .where("$COL_ID = ?")
            .whereArgs(obj.id)
            .build()

    override fun mapToContentValues(obj: PendingDeleteItem) = ContentValues(2).apply {
        put(COL_ID, obj.id)
        put(COL_CHAPTER_ID, obj.chapter_id)
    }
}

class PendingDeleteGetResolver : DefaultGetResolver<PendingDeleteItem>() {

    override fun mapFromCursor(cursor: Cursor): PendingDeleteItem =
            PendingDeleteItemImpl(cursor.getLong(cursor.getColumnIndex(COL_CHAPTER_ID)))
                    .apply {
                        id = cursor.getLong(cursor.getColumnIndex(COL_ID))
                    }
}

class PendingDeleteDeleteResolver : DefaultDeleteResolver<PendingDeleteItem>() {

    override fun mapToDeleteQuery(obj: PendingDeleteItem) = DeleteQuery.builder()
            .table(TABLE)
            .where("$COL_ID = ?")
            .whereArgs(obj.id)
            .build()
}