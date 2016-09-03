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
import eu.kanade.tachiyomi.data.database.models.MangaSync
import eu.kanade.tachiyomi.data.database.models.MangaSyncImpl
import eu.kanade.tachiyomi.data.database.tables.MangaSyncTable.COL_ID
import eu.kanade.tachiyomi.data.database.tables.MangaSyncTable.COL_IS_BIND
import eu.kanade.tachiyomi.data.database.tables.MangaSyncTable.COL_LAST_CHAPTER_READ
import eu.kanade.tachiyomi.data.database.tables.MangaSyncTable.COL_MANGA_ID
import eu.kanade.tachiyomi.data.database.tables.MangaSyncTable.COL_REMOTE_ID
import eu.kanade.tachiyomi.data.database.tables.MangaSyncTable.COL_REMOTE_SCORE
import eu.kanade.tachiyomi.data.database.tables.MangaSyncTable.COL_SCORE
import eu.kanade.tachiyomi.data.database.tables.MangaSyncTable.COL_STATUS
import eu.kanade.tachiyomi.data.database.tables.MangaSyncTable.COL_SYNC_ID
import eu.kanade.tachiyomi.data.database.tables.MangaSyncTable.COL_TITLE
import eu.kanade.tachiyomi.data.database.tables.MangaSyncTable.COL_TOTAL_CHAPTERS
import eu.kanade.tachiyomi.data.database.tables.MangaSyncTable.TABLE

class MangaSyncTypeMapping : SQLiteTypeMapping<MangaSync>(
        MangaSyncPutResolver(),
        MangaSyncGetResolver(),
        MangaSyncDeleteResolver()
)

class MangaSyncPutResolver : DefaultPutResolver<MangaSync>() {

    override fun mapToInsertQuery(obj: MangaSync) = InsertQuery.builder()
            .table(TABLE)
            .build()

    override fun mapToUpdateQuery(obj: MangaSync) = UpdateQuery.builder()
            .table(TABLE)
            .where("$COL_ID = ?")
            .whereArgs(obj.id)
            .build()

    override fun mapToContentValues(obj: MangaSync) = ContentValues(11).apply {
        put(COL_ID, obj.id)
        put(COL_MANGA_ID, obj.manga_id)
        put(COL_SYNC_ID, obj.sync_id)
        put(COL_REMOTE_ID, obj.remote_id)
        put(COL_REMOTE_SCORE, obj.remote_score)
        put(COL_TITLE, obj.title)
        put(COL_LAST_CHAPTER_READ, obj.last_chapter_read)
        put(COL_TOTAL_CHAPTERS, obj.total_chapters)
        put(COL_STATUS, obj.status)
        put(COL_SCORE, obj.score)
        put(COL_IS_BIND, if (obj.is_bind) 1 else 0)
    }
}

class MangaSyncGetResolver : DefaultGetResolver<MangaSync>() {

    override fun mapFromCursor(cursor: Cursor): MangaSync = MangaSyncImpl().apply {
        id = cursor.getLong(cursor.getColumnIndex(COL_ID))
        manga_id = cursor.getLong(cursor.getColumnIndex(COL_MANGA_ID))
        sync_id = cursor.getInt(cursor.getColumnIndex(COL_SYNC_ID))
        remote_id = cursor.getInt(cursor.getColumnIndex(COL_REMOTE_ID))
        remote_score = cursor.getFloat(cursor.getColumnIndex(COL_REMOTE_SCORE))
        title = cursor.getString(cursor.getColumnIndex(COL_TITLE))
        last_chapter_read = cursor.getInt(cursor.getColumnIndex(COL_LAST_CHAPTER_READ))
        total_chapters = cursor.getInt(cursor.getColumnIndex(COL_TOTAL_CHAPTERS))
        status = cursor.getInt(cursor.getColumnIndex(COL_STATUS))
        score = cursor.getFloat(cursor.getColumnIndex(COL_SCORE))
        is_bind = cursor.getInt(cursor.getColumnIndex(COL_IS_BIND)) == 1
    }
}

class MangaSyncDeleteResolver : DefaultDeleteResolver<MangaSync>() {

    override fun mapToDeleteQuery(obj: MangaSync) = DeleteQuery.builder()
            .table(TABLE)
            .where("$COL_ID = ?")
            .whereArgs(obj.id)
            .build()
}
