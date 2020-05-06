package eu.kanade.tachiyomi.util

import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.LocalSource
import java.util.Date

fun Manga.isLocal() = source == LocalSource.ID

/**
 * Call before updating [Manga.thumbnail_url] to ensure old cover can be cleared from cache
 */
fun Manga.prepUpdateCover(coverCache: CoverCache) {
    metadata_date = Date().time

    if (!isLocal()) {
        coverCache.deleteFromCache(this, false)
    }
}

fun Manga.removeCovers(coverCache: CoverCache) {
    if (isLocal()) return

    metadata_date = Date().time
    coverCache.deleteFromCache(this, true)
}

fun Manga.updateMetadataDate(db: DatabaseHelper) {
    metadata_date = Date().time
    db.updateMangaMetadataDate(this).executeAsBlocking()
}
