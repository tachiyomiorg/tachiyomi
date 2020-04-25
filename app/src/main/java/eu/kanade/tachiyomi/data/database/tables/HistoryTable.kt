package eu.kanade.tachiyomi.data.database.tables

object HistoryTable {

    /**
     * Table name
     */
    const val TABLE = "history"

    /**
     * Id column name
     */
    const val COL_ID = "${TABLE}_id"

    /**
     * Chapter id column name
     */
    const val COL_CHAPTER_ID = "${TABLE}_chapter_id"

    /**
     * Last read column name
     */
    const val COL_LAST_READ = "${TABLE}_last_read"

    /**
     * Time read column name
     */
    const val COL_TIME_READ = "${TABLE}_time_read"

    /**
     * query to create history table
     */
    val createTableQuery: String
        get() =
            """CREATE TABLE $TABLE(
            $COL_ID INTEGER NOT NULL PRIMARY KEY,
            $COL_CHAPTER_ID INTEGER NOT NULL UNIQUE,
            $COL_LAST_READ LONG,
            $COL_TIME_READ LONG,
            FOREIGN KEY($COL_CHAPTER_ID) REFERENCES ${ChapterTable.TABLE} (${ChapterTable.COL_ID})
            ON DELETE CASCADE
            )"""

    /**
     * query to index history chapter id
     */
    val createChapterIdIndexQuery: String
        get() = "CREATE INDEX ${TABLE}_${COL_CHAPTER_ID}_index ON $TABLE($COL_CHAPTER_ID)"
}
