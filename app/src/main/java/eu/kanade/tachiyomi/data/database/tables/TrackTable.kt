package eu.kanade.tachiyomi.data.database.tables

object TrackTable {

    const val TABLE = "manga_sync"

    const val COL_ID = "_id"

    const val COL_MANGA_ID = "manga_id"

    const val COL_SYNC_ID = "sync_id"

    const val COL_MEDIA_ID = "media_id"

    const val COL_LIBRARY_ID = "library_id"

    const val COL_TITLE = "title"

    const val COL_LAST_CHAPTER_READ = "last_chapter_read"

    const val COL_STATUS = "status"

    const val COL_SCORE = "score"

    const val COL_TOTAL_CHAPTERS = "total_chapters"

    const val COL_TRACKING_URL = "remote_url"

    val createTableQuery: String
        get() = """CREATE TABLE $TABLE(
            $COL_ID INTEGER NOT NULL PRIMARY KEY,
            $COL_MANGA_ID INTEGER NOT NULL,
            $COL_SYNC_ID INTEGER NOT NULL,
            $COL_MEDIA_ID INTEGER NOT NULL,
            $COL_LIBRARY_ID INTEGER,
            $COL_TITLE TEXT NOT NULL,
            $COL_LAST_CHAPTER_READ INTEGER NOT NULL,
            $COL_TOTAL_CHAPTERS INTEGER NOT NULL,
            $COL_STATUS INTEGER NOT NULL,
            $COL_SCORE FLOAT NOT NULL,
            $COL_TRACKING_URL TEXT NOT NULL,
            UNIQUE ($COL_MANGA_ID, $COL_SYNC_ID) ON CONFLICT REPLACE,
            FOREIGN KEY($COL_MANGA_ID) REFERENCES ${MangaTable.TABLE} (${MangaTable.COL_ID})
            ON DELETE CASCADE
            )"""

    val addTrackingUrl: String
        get() = "ALTER TABLE $TABLE ADD COLUMN $COL_TRACKING_URL TEXT DEFAULT ''"

    val copyOldTable: String
        get() = """
           ALTER TABLE $TABLE RENAME TO backup
        """
    val copyOldData: String
        get() = """
            INSERT INTO $TABLE(
            $COL_ID,
            $COL_MANGA_ID,
            $COL_SYNC_ID,
            $COL_MEDIA_ID,
            $COL_TITLE,
            $COL_LAST_CHAPTER_READ,
            $COL_TOTAL_CHAPTERS,
            $COL_STATUS,
            $COL_SCORE,
            $COL_TRACKING_URL)
            SELECT $COL_ID,
            $COL_MANGA_ID,
            $COL_SYNC_ID,
            remote_id,
            $COL_TITLE,
            $COL_LAST_CHAPTER_READ,
            $COL_TOTAL_CHAPTERS,
            $COL_STATUS,
            $COL_SCORE,
            $COL_TRACKING_URL
            FROM backup
            """

    val dropOldTable: String
        get() = "DROP TABLE backup"
}
