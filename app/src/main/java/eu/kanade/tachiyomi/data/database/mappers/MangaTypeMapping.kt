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
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_ARTIST
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_AUTHOR
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_CHAPTER_FLAGS
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_COVER_LAST_MODIFIED
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_DATE_ADDED
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_DESCRIPTION
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_FAVORITE
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_GENRE
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_ID
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_INITIALIZED
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_LAST_UPDATE
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_NEXT_UPDATE
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_SOURCE
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_STATUS
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_THUMBNAIL_URL
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_TITLE
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_URL
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_VIEWER
import eu.kanade.tachiyomi.data.database.tables.MangaTable.TABLE

class MangaTypeMapping : SQLiteTypeMapping<Manga>(
    MangaPutResolver(),
    MangaGetResolver(),
    MangaDeleteResolver()
)

class MangaPutResolver : DefaultPutResolver<Manga>() {

    override fun mapToInsertQuery(obj: Manga) = InsertQuery.builder()
        .table(TABLE)
        .build()

    override fun mapToUpdateQuery(obj: Manga) = UpdateQuery.builder()
        .table(TABLE)
        .where("$COL_ID = ?")
        .whereArgs(obj.id)
        .build()

    override fun mapToContentValues(obj: Manga) =
        contentValuesOf(
            COL_ID to obj.id,
            COL_SOURCE to obj.source,
            COL_URL to obj.url,
            COL_ARTIST to obj.artist,
            COL_AUTHOR to obj.author,
            COL_DESCRIPTION to obj.description,
            COL_GENRE to obj.genre,
            COL_TITLE to obj.title,
            COL_STATUS to obj.status,
            COL_THUMBNAIL_URL to obj.thumbnail_url,
            COL_FAVORITE to obj.favorite,
            COL_LAST_UPDATE to obj.last_update,
            COL_NEXT_UPDATE to obj.next_update,
            COL_INITIALIZED to obj.initialized,
            COL_VIEWER to obj.viewer_flags,
            COL_CHAPTER_FLAGS to obj.chapter_flags,
            COL_COVER_LAST_MODIFIED to obj.cover_last_modified,
            COL_DATE_ADDED to obj.date_added
        )
}

interface BaseMangaGetResolver {
    fun mapBaseFromCursor(manga: Manga, cursor: Cursor) = manga.apply {
        id = cursor.getLong(cursor.getColumnIndex(COL_ID))
        source = cursor.getLong(cursor.getColumnIndex(COL_SOURCE))
        url = cursor.getString(cursor.getColumnIndex(COL_URL))
        artist = cursor.getString(cursor.getColumnIndex(COL_ARTIST))
        author = cursor.getString(cursor.getColumnIndex(COL_AUTHOR))
        description = cursor.getString(cursor.getColumnIndex(COL_DESCRIPTION))
        genre = cursor.getString(cursor.getColumnIndex(COL_GENRE))
        title = cursor.getString(cursor.getColumnIndex(COL_TITLE))
        status = cursor.getInt(cursor.getColumnIndex(COL_STATUS))
        thumbnail_url = cursor.getString(cursor.getColumnIndex(COL_THUMBNAIL_URL))
        favorite = cursor.getInt(cursor.getColumnIndex(COL_FAVORITE)) == 1
        last_update = cursor.getLong(cursor.getColumnIndex(COL_LAST_UPDATE))
        next_update = cursor.getLong(cursor.getColumnIndex(COL_NEXT_UPDATE))
        initialized = cursor.getInt(cursor.getColumnIndex(COL_INITIALIZED)) == 1
        viewer_flags = cursor.getInt(cursor.getColumnIndex(COL_VIEWER))
        chapter_flags = cursor.getInt(cursor.getColumnIndex(COL_CHAPTER_FLAGS))
        cover_last_modified = cursor.getLong(cursor.getColumnIndex(COL_COVER_LAST_MODIFIED))
        date_added = cursor.getLong(cursor.getColumnIndex(COL_DATE_ADDED))
    }
}

open class MangaGetResolver : DefaultGetResolver<Manga>(), BaseMangaGetResolver {

    override fun mapFromCursor(cursor: Cursor): Manga {
        return mapBaseFromCursor(MangaImpl(), cursor)
    }
}

class MangaDeleteResolver : DefaultDeleteResolver<Manga>() {

    override fun mapToDeleteQuery(obj: Manga) = DeleteQuery.builder()
        .table(TABLE)
        .where("$COL_ID = ?")
        .whereArgs(obj.id)
        .build()
}
