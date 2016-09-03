package eu.kanade.tachiyomi.data.database.tables

object MangaSyncTable {

    const val TABLE = "manga_sync"

    const val COL_ID = "_id"

    const val COL_MANGA_ID = "manga_id"

    const val COL_SYNC_ID = "sync_id"

    const val COL_REMOTE_ID = "remote_id"

    const val COL_REMOTE_SCORE = "remote_score"

    const val COL_TITLE = "title"

    const val COL_LAST_CHAPTER_READ = "last_chapter_read"

    const val COL_STATUS = "status"

    const val COL_SCORE = "score"

    const val COL_TOTAL_CHAPTERS = "total_chapters"

    const val COL_IS_BIND = "is_bind"

    val createTableQuery: String
        get() = """CREATE TABLE $TABLE(
            $COL_ID INTEGER NOT NULL PRIMARY KEY,
            $COL_MANGA_ID INTEGER NOT NULL,
            $COL_SYNC_ID INTEGER NOT NULL,
            $COL_REMOTE_ID INTEGER NOT NULL,
            $COL_REMOTE_SCORE FLOAT NOT NULL,
            $COL_TITLE TEXT NOT NULL,
            $COL_LAST_CHAPTER_READ INTEGER NOT NULL,
            $COL_TOTAL_CHAPTERS INTEGER NOT NULL,
            $COL_STATUS INTEGER NOT NULL,
            $COL_SCORE FLOAT NOT NULL,
            $COL_IS_BIND INTEGER NOT NULL,
            UNIQUE ($COL_MANGA_ID, $COL_SYNC_ID) ON CONFLICT REPLACE,
            FOREIGN KEY($COL_MANGA_ID) REFERENCES ${MangaTable.TABLE} (${MangaTable.COL_ID})
            ON DELETE CASCADE
            )"""

    val remoteScoreUpdateQuery: String
        get() = "ALTER TABLE $TABLE ADD COLUMN $COL_REMOTE_SCORE FLOAT DEFAULT 0"

    val isBindUpdateQuery: String
        get() = "ALTER TABLE $TABLE ADD COLUMN $COL_IS_BIND INTEGER DEFAULT 0"

    val updateIsBind: String
        get() = "UPDATE $TABLE SET $COL_IS_BIND = REPLACE($COL_IS_BIND, 0, 1)"
}
