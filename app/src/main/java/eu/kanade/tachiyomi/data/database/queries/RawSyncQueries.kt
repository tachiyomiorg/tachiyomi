package eu.kanade.tachiyomi.data.database.queries

import eu.kanade.tachiyomi.data.database.tables.MangaCategoryTable

/**
 * Take a snapshot of the manga category table
 */
//language=sql
fun cloneMangaCategoriesQuery(id: String) = """
    CREATE TABLE ${MangaCategoryTable.SNAPSHOT_TABLE_PREFIX}$id SELECT * FROM ${MangaCategoryTable.TABLE}
        """

/**
 * Delete the snapshot of the manga category table
 */
//language=sql
fun deleteClonedMangaCategoriesQuery(id: String) = """
    DROP TABLE IF EXISTS ${MangaCategoryTable.SNAPSHOT_TABLE_PREFIX}$id
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
fun getDeletedMangaCategoriesQuery(id: String) = genDiffMangaCategoriesQuery(
        MangaCategoryTable.SNAPSHOT_TABLE_PREFIX + id,
        MangaCategoryTable.TABLE
)

/**
 * Find manga categories that have been added since the last snapshot
 */
//Deleted categories exist in current table and are missing from last snapshot
fun getAddedMangaCategoriesQuery(id: String) = genDiffMangaCategoriesQuery(
        MangaCategoryTable.TABLE,
        MangaCategoryTable.SNAPSHOT_TABLE_PREFIX + id
)
