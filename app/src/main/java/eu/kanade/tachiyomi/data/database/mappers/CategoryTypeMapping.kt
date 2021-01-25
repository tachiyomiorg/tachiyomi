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
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.CategoryImpl
import eu.kanade.tachiyomi.data.database.tables.CategoryTable.COL_FLAGS
import eu.kanade.tachiyomi.data.database.tables.CategoryTable.COL_ID
import eu.kanade.tachiyomi.data.database.tables.CategoryTable.COL_NAME
import eu.kanade.tachiyomi.data.database.tables.CategoryTable.COL_ORDER
import eu.kanade.tachiyomi.data.database.tables.CategoryTable.TABLE

class CategoryTypeMapping : SQLiteTypeMapping<Category>(
    CategoryPutResolver(),
    CategoryGetResolver(),
    CategoryDeleteResolver()
)

class CategoryPutResolver : DefaultPutResolver<Category>() {

    override fun mapToInsertQuery(obj: Category) = InsertQuery.builder()
        .table(TABLE)
        .build()

    override fun mapToUpdateQuery(obj: Category) = UpdateQuery.builder()
        .table(TABLE)
        .where("$COL_ID = ?")
        .whereArgs(obj.id)
        .build()

    override fun mapToContentValues(obj: Category) =
        contentValuesOf(
            COL_ID to obj.id,
            COL_NAME to obj.name,
            COL_ORDER to obj.order,
            COL_FLAGS to obj.flags
        )
}

class CategoryGetResolver : DefaultGetResolver<Category>() {

    override fun mapFromCursor(cursor: Cursor): Category = CategoryImpl().apply {
        id = cursor.getInt(cursor.getColumnIndex(COL_ID))
        name = cursor.getString(cursor.getColumnIndex(COL_NAME))
        order = cursor.getInt(cursor.getColumnIndex(COL_ORDER))
        flags = cursor.getInt(cursor.getColumnIndex(COL_FLAGS))
    }
}

class CategoryDeleteResolver : DefaultDeleteResolver<Category>() {

    override fun mapToDeleteQuery(obj: Category) = DeleteQuery.builder()
        .table(TABLE)
        .where("$COL_ID = ?")
        .whereArgs(obj.id)
        .build()
}
