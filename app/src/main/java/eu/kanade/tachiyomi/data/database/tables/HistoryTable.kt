package eu.kanade.tachiyomi.data.database.tables

object HistoryTable {

    const val TABLE = "history"

    const val COL_ID = "_id"

    const val COL_MANGA_ID = "manga_id"

    const val COL_LAST_READ = "last_read"


    val createTableQuery: String
        get() = """CREATE TABLE $TABLE(
            $COL_ID INTEGER NOT NULL PRIMARY KEY,
            $COL_MANGA_ID INTEGER NOT NULL,
            $COL_LAST_READ LONG,
            FOREIGN KEY($COL_MANGA_ID) REFERENCES ${MangaTable.TABLE} (${MangaTable.COL_ID})
            ON DELETE CASCADE
            )"""

    val createMangaIdIndexQuery: String
        get() = "CREATE INDEX ${HistoryTable.TABLE}_${HistoryTable.COL_MANGA_ID}_index ON ${HistoryTable.TABLE}(${HistoryTable.COL_MANGA_ID})"
}
