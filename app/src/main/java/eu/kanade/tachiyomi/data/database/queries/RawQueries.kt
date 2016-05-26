package eu.kanade.tachiyomi.data.database.queries

import eu.kanade.tachiyomi.data.database.tables.CategoryTable as Category
import eu.kanade.tachiyomi.data.database.tables.ChapterTable as Chapter
import eu.kanade.tachiyomi.data.database.tables.HistoryTable as History
import eu.kanade.tachiyomi.data.database.tables.MangaCategoryTable as MangaCategory
import eu.kanade.tachiyomi.data.database.tables.MangaTable as Manga

/**
 * Query to get the manga from the library, with their categories and unread count.
 */
val libraryQuery = """
    SELECT M.*, COALESCE(MC.${MangaCategory.COL_CATEGORY_ID}, 0) AS ${Manga.COL_CATEGORY}
    FROM (
        SELECT ${Manga.TABLE}.*, COALESCE(C.unread, 0) AS ${Manga.COL_UNREAD}
        FROM ${Manga.TABLE}
        LEFT JOIN (
            SELECT ${Chapter.COL_MANGA_ID}, COUNT(*) AS unread
            FROM ${Chapter.TABLE}
            WHERE ${Chapter.COL_READ} = 0
            GROUP BY ${Chapter.COL_MANGA_ID}
        ) AS C
        ON ${Manga.COL_ID} = C.${Chapter.COL_MANGA_ID}
        WHERE ${Manga.COL_FAVORITE} = 1
        GROUP BY ${Manga.COL_ID}
        ORDER BY ${Manga.COL_TITLE}
    ) AS M
    LEFT JOIN (
        SELECT * FROM ${MangaCategory.TABLE}) AS MC
        ON MC.${MangaCategory.COL_MANGA_ID} = M.${Manga.COL_ID}
"""

/**
 * Query to get the recent chapters of manga from the library up to a date.
 */
fun getRecentsQuery() = """
    SELECT ${Manga.TABLE}.${Manga.COL_URL} as mangaUrl, * FROM ${Manga.TABLE} JOIN ${Chapter.TABLE}
    ON ${Manga.TABLE}.${Manga.COL_ID} = ${Chapter.TABLE}.${Chapter.COL_MANGA_ID}
    WHERE ${Manga.COL_FAVORITE} = 1 AND ${Chapter.COL_DATE_UPLOAD} > ?
    ORDER BY ${Chapter.COL_DATE_UPLOAD} DESC
"""

/**
 * Query to get the recent chapters of manga from the library up to a date.
 */
fun getRecentMangasQuery() = """
    SELECT ${Manga.TABLE}.* FROM ${Manga.TABLE} JOIN ${History.TABLE}
    ON ${Manga.TABLE}.${Manga.COL_ID} = ${History.TABLE}.${History.COL_MANGA_ID}
    WHERE ${History.COL_LAST_READ} > ?
    ORDER BY ${History.COL_LAST_READ} DESC
"""


/**
 * Query to get the categories for a manga.
 */
fun getCategoriesForMangaQuery() = """
    SELECT ${Category.TABLE}.* FROM ${Category.TABLE}
    JOIN ${MangaCategory.TABLE} ON ${Category.TABLE}.${Category.COL_ID} =
    ${MangaCategory.TABLE}.${MangaCategory.COL_CATEGORY_ID}
    WHERE ${MangaCategory.COL_MANGA_ID} = ?
"""