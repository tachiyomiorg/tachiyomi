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
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.database.tables.MangaCategoryTable.COL_CATEGORY_ID
import eu.kanade.tachiyomi.data.database.tables.MangaCategoryTable.COL_ID
import eu.kanade.tachiyomi.data.database.tables.MangaCategoryTable.COL_MANGA_ID
import eu.kanade.tachiyomi.data.database.tables.MangaCategoryTable.TABLE

class MangaCategoryTypeMapping : SQLiteTypeMapping<MangaCategory>(
    MangaCategoryPutResolver(),
    MangaCategoryGetResolver(),
    MangaCategoryDeleteResolver()
)

class MangaCategoryPutResolver : DefaultPutResolver<MangaCategory>() {

    override fun mapToInsertQuery(obj: MangaCategory) = InsertQuery.builder()
        .table(TABLE)
        .build()

    override fun mapToUpdateQuery(obj: MangaCategory) = UpdateQuery.builder()
        .table(TABLE)
        .where("$COL_ID = ?")
        .whereArgs(obj.id)
        .build()

    override fun mapToContentValues(obj: MangaCategory) =
        contentValuesOf(
            COL_ID to obj.id,
            COL_MANGA_ID to obj.manga_id,
            COL_CATEGORY_ID to obj.category_id
        )
}

class MangaCategoryGetResolver : DefaultGetResolver<MangaCategory>() {

    override fun mapFromCursor(cursor: Cursor): MangaCategory = MangaCategory().apply {
        id = cursor.getLong(cursor.getColumnIndex(COL_ID))
        manga_id = cursor.getLong(cursor.getColumnIndex(COL_MANGA_ID))
        category_id = cursor.getInt(cursor.getColumnIndex(COL_CATEGORY_ID))
    }
}

class MangaCategoryDeleteResolver : DefaultDeleteResolver<MangaCategory>() {

    override fun mapToDeleteQuery(obj: MangaCategory) = DeleteQuery.builder()
        .table(TABLE)
        .where("$COL_ID = ?")
        .whereArgs(obj.id)
        .build()
}
