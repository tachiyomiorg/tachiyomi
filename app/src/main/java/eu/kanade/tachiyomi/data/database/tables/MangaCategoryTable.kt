package eu.kanade.tachiyomi.data.database.tables

object MangaCategoryTable {

    const val TABLE = "mangas_categories"

    const val COL_ID = "_id"

    const val COL_MANGA_ID = "manga_id"

    const val COL_CATEGORY_ID = "category_id"

    val createTableQuery: String
        get() = """CREATE TABLE $TABLE(
            $COL_ID INTEGER NOT NULL PRIMARY KEY,
            $COL_MANGA_ID INTEGER NOT NULL,
            $COL_CATEGORY_ID INTEGER NOT NULL,
            FOREIGN KEY($COL_CATEGORY_ID) REFERENCES ${CategoryTable.TABLE} (${CategoryTable.COL_ID})
            ON DELETE CASCADE,
            FOREIGN KEY($COL_MANGA_ID) REFERENCES ${MangaTable.TABLE} (${MangaTable.COL_ID})
            ON DELETE CASCADE
            )"""
    /**
     * Query to update uncategorized manga to a category when updating to All Category version
     */
    val addMangaCategoryOnUpdateToAll: String
        get() = """INSERT INTO ${MangaCategoryTable.TABLE}(
			${MangaCategoryTable.COL_MANGA_ID},
			${MangaCategoryTable.COL_CATEGORY_ID}
			)
        select
             ${MangaTable.TABLE}.${MangaTable.COL_ID},
            9999
         from
                ${MangaTable.TABLE}
          where
          ${MangaTable.TABLE}.${MangaTable.COL_ID} in (
				SELECT ${MangaTable.TABLE}.${MangaTable.COL_ID} FROM ${MangaTable.TABLE}
				LEFT JOIN ${MangaCategoryTable.TABLE} ON
                    ${MangaTable.TABLE}.${MangaTable.COL_ID} = ${MangaCategoryTable.TABLE}.${MangaCategoryTable.COL_MANGA_ID}
				WHERE ${MangaCategoryTable.TABLE}.${MangaCategoryTable.COL_MANGA_ID} IS NULL)
        """
}
