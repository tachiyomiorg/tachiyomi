package eu.kanade.tachiyomi.data.database.queries

import eu.kanade.tachiyomi.data.database.tables.MangaCategoryTable

/**
 * Take a snapshot of the manga category table
 */
//language=sql
val cloneMangaCategoriesQuery = """
    CREATE TABLE ${MangaCategoryTable.SNAPSHOT_TABLE} SELECT * FROM ${MangaCategoryTable.TABLE}
        """

/**
 * Delete the snapshot of the manga category table
 */
//language=sql
val deleteClonedMangaCategoriesQuery = """
    DROP TABLE IF EXISTS ${MangaCategoryTable.SNAPSHOT_TABLE}
        """

/**
 * Finds manga categories that exist in table `from` and do not exist in table `to`
 */
//language=sql
private fun genDiffMangaCategoriesQuery(from: String,
                                        to: String) = """
    SELECT * FROM $from WHERE NOT EXIST
   (SELECT * FROM $to WHERE $from.${MangaCategoryTable.COL_MANGA_ID} = $to.${MangaCategoryTable.COL_MANGA_ID}
       AND $from.${MangaCategoryTable.COL_CATEGORY_ID} = $to.${MangaCategoryTable.COL_CATEGORY_ID})
    """

/**
 * Find manga categories that have been deleted since the last snapshot
 */
//Deleted categories exist in last snapshot and are missing from current table
val getDeletedMangaCategoriesQuery = genDiffMangaCategoriesQuery(
        MangaCategoryTable.SNAPSHOT_TABLE,
        MangaCategoryTable.TABLE
)

/**
 * Find manga categories that have been added since the last snapshot
 */
//Deleted categories exist in current table and are missing from last snapshot
val getAddedMangaCategoriesQuery = genDiffMangaCategoriesQuery(
        MangaCategoryTable.TABLE,
        MangaCategoryTable.SNAPSHOT_TABLE
)
