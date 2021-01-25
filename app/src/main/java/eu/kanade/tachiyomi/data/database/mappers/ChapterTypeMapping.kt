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
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.ChapterImpl
import eu.kanade.tachiyomi.data.database.tables.ChapterTable.COL_BOOKMARK
import eu.kanade.tachiyomi.data.database.tables.ChapterTable.COL_CHAPTER_NUMBER
import eu.kanade.tachiyomi.data.database.tables.ChapterTable.COL_DATE_FETCH
import eu.kanade.tachiyomi.data.database.tables.ChapterTable.COL_DATE_UPLOAD
import eu.kanade.tachiyomi.data.database.tables.ChapterTable.COL_ID
import eu.kanade.tachiyomi.data.database.tables.ChapterTable.COL_LAST_PAGE_READ
import eu.kanade.tachiyomi.data.database.tables.ChapterTable.COL_MANGA_ID
import eu.kanade.tachiyomi.data.database.tables.ChapterTable.COL_NAME
import eu.kanade.tachiyomi.data.database.tables.ChapterTable.COL_READ
import eu.kanade.tachiyomi.data.database.tables.ChapterTable.COL_SCANLATOR
import eu.kanade.tachiyomi.data.database.tables.ChapterTable.COL_SOURCE_ORDER
import eu.kanade.tachiyomi.data.database.tables.ChapterTable.COL_URL
import eu.kanade.tachiyomi.data.database.tables.ChapterTable.TABLE

class ChapterTypeMapping : SQLiteTypeMapping<Chapter>(
    ChapterPutResolver(),
    ChapterGetResolver(),
    ChapterDeleteResolver()
)

class ChapterPutResolver : DefaultPutResolver<Chapter>() {

    override fun mapToInsertQuery(obj: Chapter) = InsertQuery.builder()
        .table(TABLE)
        .build()

    override fun mapToUpdateQuery(obj: Chapter) = UpdateQuery.builder()
        .table(TABLE)
        .where("$COL_ID = ?")
        .whereArgs(obj.id)
        .build()

    override fun mapToContentValues(obj: Chapter) =
        contentValuesOf(
            COL_ID to obj.id,
            COL_MANGA_ID to obj.manga_id,
            COL_URL to obj.url,
            COL_NAME to obj.name,
            COL_READ to obj.read,
            COL_SCANLATOR to obj.scanlator,
            COL_BOOKMARK to obj.bookmark,
            COL_DATE_FETCH to obj.date_fetch,
            COL_DATE_UPLOAD to obj.date_upload,
            COL_LAST_PAGE_READ to obj.last_page_read,
            COL_CHAPTER_NUMBER to obj.chapter_number,
            COL_SOURCE_ORDER to obj.source_order
        )
}

class ChapterGetResolver : DefaultGetResolver<Chapter>() {

    override fun mapFromCursor(cursor: Cursor): Chapter = ChapterImpl().apply {
        id = cursor.getLong(cursor.getColumnIndex(COL_ID))
        manga_id = cursor.getLong(cursor.getColumnIndex(COL_MANGA_ID))
        url = cursor.getString(cursor.getColumnIndex(COL_URL))
        name = cursor.getString(cursor.getColumnIndex(COL_NAME))
        scanlator = cursor.getString(cursor.getColumnIndex(COL_SCANLATOR))
        read = cursor.getInt(cursor.getColumnIndex(COL_READ)) == 1
        bookmark = cursor.getInt(cursor.getColumnIndex(COL_BOOKMARK)) == 1
        date_fetch = cursor.getLong(cursor.getColumnIndex(COL_DATE_FETCH))
        date_upload = cursor.getLong(cursor.getColumnIndex(COL_DATE_UPLOAD))
        last_page_read = cursor.getInt(cursor.getColumnIndex(COL_LAST_PAGE_READ))
        chapter_number = cursor.getFloat(cursor.getColumnIndex(COL_CHAPTER_NUMBER))
        source_order = cursor.getInt(cursor.getColumnIndex(COL_SOURCE_ORDER))
    }
}

class ChapterDeleteResolver : DefaultDeleteResolver<Chapter>() {

    override fun mapToDeleteQuery(obj: Chapter) = DeleteQuery.builder()
        .table(TABLE)
        .where("$COL_ID = ?")
        .whereArgs(obj.id)
        .build()
}
