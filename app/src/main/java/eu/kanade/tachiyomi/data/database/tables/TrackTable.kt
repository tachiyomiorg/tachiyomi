package eu.kanade.tachiyomi.data.database.tables

object TrackTable {

    const val TABLE = "manga_sync"

    const val COL_ID = "_id"

    const val COL_MANGA_ID = "manga_id"

    const val COL_SYNC_ID = "sync_id"

    const val COL_REMOTE_ID = "remote_id"

    const val COL_TITLE = "title"

    const val COL_LAST_CHAPTER_READ = "last_chapter_read"

    const val COL_STATUS = "status"

    const val COL_SCORE = "score"

    const val COL_TOTAL_CHAPTERS = "total_chapters"

    const val COL_TRACKING_URL = "remote_url"

    const val COL_COVER_URL = "cover_url"

    val createTableQuery: String
        get() = """CREATE TABLE $TABLE(
            $COL_ID INTEGER NOT NULL PRIMARY KEY,
            $COL_MANGA_ID INTEGER NOT NULL,
            $COL_SYNC_ID INTEGER NOT NULL,
            $COL_REMOTE_ID INTEGER NOT NULL,
            $COL_TITLE TEXT NOT NULL,
            $COL_LAST_CHAPTER_READ INTEGER NOT NULL,
            $COL_TOTAL_CHAPTERS INTEGER NOT NULL,
            $COL_STATUS INTEGER NOT NULL,
            $COL_SCORE FLOAT NOT NULL,
            $COL_TRACKING_URL TEXT NOT NULL,
            $COL_COVER_URL TEXT NOT NULL,
            UNIQUE ($COL_MANGA_ID, $COL_SYNC_ID) ON CONFLICT REPLACE,
            FOREIGN KEY($COL_MANGA_ID) REFERENCES ${MangaTable.TABLE} (${MangaTable.COL_ID})
            ON DELETE CASCADE
            )"""

    val addTrackingUrl: String
        get() = "ALTER TABLE $TABLE ADD COLUMN $COL_TRACKING_URL TEXT DEFAULT ''"

    val addCoverUrl: String
        get() = "ALTER TABLE $TABLE ADD COLUMN $COL_COVER_URL TEXT DEFAULT ''"
}
