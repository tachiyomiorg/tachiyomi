package eu.kanade.tachiyomi.data.database.tables

object SyncUpdatesTable {
    const val TABLE = "sync_updates"

    const val COL_ID = "_id"

    const val COL_UPDATED_ROW = "updated_row"

    const val COL_DATETIME = "datetime"

    const val COL_FIELD = "field"

    val createTableQuery: String
        // language=sql
        get() = """CREATE TABLE $TABLE(
            $COL_ID INTEGER NOT NULL PRIMARY KEY,
            $COL_UPDATED_ROW INTEGER NOT NULL,
            $COL_DATETIME INTEGER NOT NULL,
            $COL_FIELD INTEGER NOT NULL
            )"""
}
