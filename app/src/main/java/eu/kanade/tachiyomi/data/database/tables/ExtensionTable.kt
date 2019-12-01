package eu.kanade.tachiyomi.data.database.tables

object ExtensionTable {

    const val TABLE = "extension"

    const val COL_ID = "_id"

    const val COL_ISFAVORITE = "isFavorite"

    val createTableQuery: String
        get() = """CREATE TABLE $TABLE(
            $COL_ID INTEGER NOT NULL PRIMARY KEY,
            $COL_ISFAVORITE BOOLEAN NOT NULL
            )"""
}
