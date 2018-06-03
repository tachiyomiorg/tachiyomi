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
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.database.models.TrackImpl
import eu.kanade.tachiyomi.data.database.tables.TrackTable.COL_ID
import eu.kanade.tachiyomi.data.database.tables.TrackTable.COL_LAST_CHAPTER_READ
import eu.kanade.tachiyomi.data.database.tables.TrackTable.COL_LIBRARY_ID
import eu.kanade.tachiyomi.data.database.tables.TrackTable.COL_MANGA_ID
import eu.kanade.tachiyomi.data.database.tables.TrackTable.COL_MEDIA_ID
import eu.kanade.tachiyomi.data.database.tables.TrackTable.COL_SCORE
import eu.kanade.tachiyomi.data.database.tables.TrackTable.COL_STATUS
import eu.kanade.tachiyomi.data.database.tables.TrackTable.COL_SYNC_ID
import eu.kanade.tachiyomi.data.database.tables.TrackTable.COL_TITLE
import eu.kanade.tachiyomi.data.database.tables.TrackTable.COL_TOTAL_CHAPTERS
import eu.kanade.tachiyomi.data.database.tables.TrackTable.COL_TRACKING_URL
import eu.kanade.tachiyomi.data.database.tables.TrackTable.TABLE

class TrackTypeMapping : SQLiteTypeMapping<Track>(
        TrackPutResolver(),
        TrackGetResolver(),
        TrackDeleteResolver()
)

class TrackPutResolver : DefaultPutResolver<Track>() {

    override fun mapToInsertQuery(obj: Track) = InsertQuery.builder()
            .table(TABLE)
            .build()

    override fun mapToUpdateQuery(obj: Track) = UpdateQuery.builder()
            .table(TABLE)
            .where("$COL_ID = ?")
            .whereArgs(obj.id)
            .build()

    override fun mapToContentValues(obj: Track) = ContentValues(10).apply {
        put(COL_ID, obj.id)
        put(COL_MANGA_ID, obj.manga_id)
        put(COL_SYNC_ID, obj.sync_id)
        put(COL_MEDIA_ID, obj.media_id)
        put(COL_LIBRARY_ID, obj.library_id)
        put(COL_TITLE, obj.title)
        put(COL_LAST_CHAPTER_READ, obj.last_chapter_read)
        put(COL_TOTAL_CHAPTERS, obj.total_chapters)
        put(COL_STATUS, obj.status)
        put(COL_TRACKING_URL, obj.tracking_url)
        put(COL_SCORE, obj.score)

    }
}

class TrackGetResolver : DefaultGetResolver<Track>() {

    override fun mapFromCursor(cursor: Cursor): Track = TrackImpl().apply {
        id = cursor.getLong(cursor.getColumnIndex(COL_ID))
        manga_id = cursor.getLong(cursor.getColumnIndex(COL_MANGA_ID))
        sync_id = cursor.getInt(cursor.getColumnIndex(COL_SYNC_ID))
        media_id = cursor.getInt(cursor.getColumnIndex(COL_MEDIA_ID))
        library_id = cursor.getLong(cursor.getColumnIndex(COL_LIBRARY_ID))
        title = cursor.getString(cursor.getColumnIndex(COL_TITLE))
        last_chapter_read = cursor.getInt(cursor.getColumnIndex(COL_LAST_CHAPTER_READ))
        total_chapters = cursor.getInt(cursor.getColumnIndex(COL_TOTAL_CHAPTERS))
        status = cursor.getInt(cursor.getColumnIndex(COL_STATUS))
        score = cursor.getFloat(cursor.getColumnIndex(COL_SCORE))
        tracking_url = cursor.getString(cursor.getColumnIndex(COL_TRACKING_URL))
    }
}

class TrackDeleteResolver : DefaultDeleteResolver<Track>() {

    override fun mapToDeleteQuery(obj: Track) = DeleteQuery.builder()
            .table(TABLE)
            .where("$COL_ID = ?")
            .whereArgs(obj.id)
            .build()
}
