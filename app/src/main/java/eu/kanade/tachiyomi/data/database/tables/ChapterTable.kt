package eu.kanade.tachiyomi.data.database.tables

object ChapterTable {

    const val TABLE = "chapters"

    const val COL_ID = "_id"

    const val COL_MANGA_ID = "manga_id"

    const val COL_URL = "url"

    const val COL_NAME = "name"

    const val COL_READ = "read"

    const val COL_SCANLATOR = "scanlator"

    const val COL_BOOKMARK = "bookmark"

    const val COL_DATE_FETCH = "date_fetch"

    const val COL_DATE_UPLOAD = "date_upload"

    const val COL_LAST_PAGE_READ = "last_page_read"

    const val COL_CHAPTER_NUMBER = "chapter_number"

    const val COL_SOURCE_ORDER = "source_order"

    val createTableQuery: String
        get() = """CREATE TABLE $TABLE(
            $COL_ID INTEGER NOT NULL PRIMARY KEY,
            $COL_MANGA_ID INTEGER NOT NULL,
            $COL_URL TEXT NOT NULL,
            $COL_NAME TEXT NOT NULL,
            $COL_SCANLATOR TEXT,
            $COL_READ BOOLEAN NOT NULL,
            $COL_BOOKMARK BOOLEAN NOT NULL,
            $COL_LAST_PAGE_READ INT NOT NULL,
            $COL_CHAPTER_NUMBER FLOAT NOT NULL,
            $COL_SOURCE_ORDER INTEGER NOT NULL,
            $COL_DATE_FETCH LONG NOT NULL,
            $COL_DATE_UPLOAD LONG NOT NULL,
            FOREIGN KEY($COL_MANGA_ID) REFERENCES ${MangaTable.TABLE} (${MangaTable.COL_ID})
            ON DELETE CASCADE
            )"""

    val createMangaIdIndexQuery: String
        get() = "CREATE INDEX ${TABLE}_${COL_MANGA_ID}_index ON $TABLE($COL_MANGA_ID)"

    val createUnreadChaptersIndexQuery: String
        get() = "CREATE INDEX ${TABLE}_unread_by_manga_index ON $TABLE($COL_MANGA_ID, $COL_READ) " +
                "WHERE $COL_READ = 0"

    val sourceOrderUpdateQuery: String
        get() = "ALTER TABLE $TABLE ADD COLUMN $COL_SOURCE_ORDER INTEGER DEFAULT 0"

    val bookmarkUpdateQuery: String
        get() = "ALTER TABLE $TABLE ADD COLUMN $COL_BOOKMARK BOOLEAN DEFAULT FALSE"

    val addScanlator: String
        get() = "ALTER TABLE $TABLE ADD COLUMN $COL_SCANLATOR TEXT DEFAULT NULL"

}
