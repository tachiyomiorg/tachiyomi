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
import eu.kanade.tachiyomi.data.database.models.Extension
import eu.kanade.tachiyomi.data.database.models.ExtensionImpl
import eu.kanade.tachiyomi.data.database.tables.ExtensionTable

class ExtensionTypeMapping : SQLiteTypeMapping<Extension>(
        ExtensionPutResolver(),
        ExtensionGetResolver(),
        ExtensionDeleteResolver()
)

class ExtensionPutResolver: DefaultPutResolver<Extension>() {

    override fun mapToInsertQuery(obj: Extension) = InsertQuery.builder()
            .table(ExtensionTable.TABLE)
            .build()

    override fun mapToUpdateQuery(obj: Extension) = UpdateQuery.builder()
            .table(ExtensionTable.TABLE)
            .where("${ExtensionTable.COL_ID} = ?")
            .whereArgs(obj.id)
            .build()

    override fun mapToContentValues(obj: Extension) = ContentValues(2).apply {
        put(ExtensionTable.COL_ID, obj.id)
        put(ExtensionTable.COL_ISFAVORITE, obj.isFavorite)

    }
}

class ExtensionGetResolver: DefaultGetResolver<Extension>() {

    override fun mapFromCursor(cursor: Cursor): Extension = ExtensionImpl().apply {
        id = cursor.getLong(cursor.getColumnIndex(ExtensionTable.COL_ID))
        isFavorite = (cursor.getInt(cursor.getColumnIndex(ExtensionTable.COL_ISFAVORITE)) == 1)

    }
}

class ExtensionDeleteResolver: DefaultDeleteResolver<Extension>() {

    override fun mapToDeleteQuery(obj: Extension) = DeleteQuery.builder()
            .table(ExtensionTable.TABLE)
            .where("${ExtensionTable.COL_ID} = ?")
            .whereArgs(obj.id)
            .build()
}
