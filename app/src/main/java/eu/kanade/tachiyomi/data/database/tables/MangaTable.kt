package eu.kanade.tachiyomi.data.database.tables

object MangaTable {

    const val TABLE = "mangas"

    const val COL_ID = "_id"

    const val COL_SOURCE = "source"

    const val COL_URL = "url"

    const val COL_ARTIST = "artist"

    const val COL_AUTHOR = "author"

    const val COL_DESCRIPTION = "description"

    const val COL_GENRE = "genre"

    const val COL_TITLE = "title"

    const val COL_STATUS = "status"

    const val COL_THUMBNAIL_URL = "thumbnail_url"

    const val COL_FAVORITE = "favorite"

    const val COL_LAST_UPDATE = "last_update"

    const val COL_NEXT_UPDATE = "next_update"

    const val COL_DATE_ADDED = "date_added"

    const val COL_INITIALIZED = "initialized"

    const val COL_VIEWER = "viewer"

    const val COL_CHAPTER_FLAGS = "chapter_flags"

    const val COL_UNREAD = "unread"

    const val COL_CATEGORY = "category"

    const val COL_COVER_LAST_MODIFIED = "cover_last_modified"

    val createTableQuery: String
        get() =
            """CREATE TABLE $TABLE(
            $COL_ID INTEGER NOT NULL PRIMARY KEY,
            $COL_SOURCE INTEGER NOT NULL,
            $COL_URL TEXT NOT NULL,
            $COL_ARTIST TEXT,
            $COL_AUTHOR TEXT,
            $COL_DESCRIPTION TEXT,
            $COL_GENRE TEXT,
            $COL_TITLE TEXT NOT NULL,
            $COL_STATUS INTEGER NOT NULL,
            $COL_THUMBNAIL_URL TEXT,
            $COL_FAVORITE INTEGER NOT NULL,
            $COL_LAST_UPDATE LONG,
            $COL_NEXT_UPDATE LONG,
            $COL_INITIALIZED BOOLEAN NOT NULL,
            $COL_VIEWER INTEGER NOT NULL,
            $COL_CHAPTER_FLAGS INTEGER NOT NULL,
            $COL_COVER_LAST_MODIFIED LONG NOT NULL,
            $COL_DATE_ADDED LONG NOT NULL
            )"""

    val createUrlIndexQuery: String
        get() = "CREATE INDEX ${TABLE}_${COL_URL}_index ON $TABLE($COL_URL)"

    val createLibraryIndexQuery: String
        get() = "CREATE INDEX library_${COL_FAVORITE}_index ON $TABLE($COL_FAVORITE) " +
            "WHERE $COL_FAVORITE = 1"

    val addCoverLastModified: String
        get() = "ALTER TABLE $TABLE ADD COLUMN $COL_COVER_LAST_MODIFIED LONG NOT NULL DEFAULT 0"

    val addDateAdded: String
        get() = "ALTER TABLE $TABLE ADD COLUMN $COL_DATE_ADDED LONG NOT NULL DEFAULT 0"

    /**
     * Used with addDateAdded to populate it with the oldest chapter fetch date.
     */
    val backfillDateAdded: String
        get() = "UPDATE $TABLE SET $COL_DATE_ADDED = " +
            "(SELECT MIN(${ChapterTable.COL_DATE_FETCH}) " +
            "FROM $TABLE INNER JOIN ${ChapterTable.TABLE} " +
            "ON $TABLE.$COL_ID = ${ChapterTable.TABLE}.${ChapterTable.COL_MANGA_ID} " +
            "GROUP BY $TABLE.$COL_ID)"

    val addNextUpdateCol: String
        get() = "ALTER TABLE $TABLE ADD COLUMN $COL_NEXT_UPDATE LONG DEFAULT 0"
}
