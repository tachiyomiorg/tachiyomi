package eu.kanade.tachiyomi.data.database.resolvers

import android.content.ContentValues
import com.pushtorefresh.storio.sqlite.StorIOSQLite
import com.pushtorefresh.storio.sqlite.operations.put.PutResolver
import com.pushtorefresh.storio.sqlite.operations.put.PutResult
import com.pushtorefresh.storio.sqlite.queries.UpdateQuery
import eu.kanade.tachiyomi.data.database.inTransactionReturn
import eu.kanade.tachiyomi.data.database.models.Extension
import eu.kanade.tachiyomi.data.database.tables.ExtensionTable


class ExtensionFavoritePutResolver : PutResolver<Extension>(){
    override fun performPut(db: StorIOSQLite, extension: Extension) = db.inTransactionReturn {
        val updateQuery = mapToUpdateQuery(extension)
        val contentValues = mapToContentValues(extension)

        val numberOfRowsUpdated = db.lowLevel().update(updateQuery, contentValues)
        PutResult.newUpdateResult(numberOfRowsUpdated, updateQuery.table())
    }

    fun mapToUpdateQuery(extension: Extension) = UpdateQuery.builder()
            .table(ExtensionTable.TABLE)
            .where("${ExtensionTable.COL_ID} = ?")
            .whereArgs(extension.id)
            .build()

    fun mapToContentValues(extension: Extension) = ContentValues(1).apply {
        put(ExtensionTable.COL_ISFAVORITE, extension.isFavorite)
    }
}
