package eu.kanade.tachiyomi.data.database.tables

object MangaCategoryTable {

    const val TABLE = "mangas_categories"

    const val COL_ID = "_id"

    const val COL_MANGA_ID = "manga_id"

    const val COL_CATEGORY_ID = "category_id"

    val createTableQuery: String
        get() =
            """CREATE TABLE $TABLE(
            $COL_ID INTEGER NOT NULL PRIMARY KEY,
            $COL_MANGA_ID INTEGER NOT NULL,
            $COL_CATEGORY_ID INTEGER NOT NULL,
            FOREIGN KEY($COL_CATEGORY_ID) REFERENCES ${CategoryTable.TABLE} (${CategoryTable.COL_ID})
            ON DELETE CASCADE,
            FOREIGN KEY($COL_MANGA_ID) REFERENCES ${MangaTable.TABLE} (${MangaTable.COL_ID})
            ON DELETE CASCADE
            )"""
}
