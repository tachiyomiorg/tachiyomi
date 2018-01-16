package eu.kanade.tachiyomi.data.database.tables

object PendingDeleteTable {

    const val TABLE = "pending_delete"

    const val COL_ID = "_id"

    const val COL_CHAPTER_ID = "chapter_id"


    val createTableQuery: String
        get() = """CREATE TABLE $TABLE(
            $COL_ID INTEGER NOT NULL PRIMARY KEY,
            $COL_CHAPTER_ID INTEGER NOT NULL
            )"""

}
