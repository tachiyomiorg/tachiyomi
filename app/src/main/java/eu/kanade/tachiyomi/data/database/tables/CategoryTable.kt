package eu.kanade.tachiyomi.data.database.tables

object CategoryTable {

    const val TABLE = "categories"

    const val COL_ID = "_id"

    const val COL_NAME = "name"

    const val COL_ORDER = "sort"

    const val COL_FLAGS = "flags"

    val createTableQuery: String
        get() = """CREATE TABLE $TABLE(
            $COL_ID INTEGER NOT NULL PRIMARY KEY,
            $COL_NAME TEXT NOT NULL,
            $COL_ORDER INTEGER NOT NULL,
            $COL_FLAGS INTEGER NOT NULL
            )"""

    val createUnCategorized: String
        get() = """INSERT INTO $TABLE(
            $COL_ID,
            $COL_NAME,
            $COL_ORDER,
            $COL_FLAGS
            )
            VALUES(
            9999,
            'Original Default',
            0,
            0
            )"""
}
