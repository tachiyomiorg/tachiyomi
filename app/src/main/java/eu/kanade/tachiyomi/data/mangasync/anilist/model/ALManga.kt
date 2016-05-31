package eu.kanade.tachiyomi.data.mangasync.anilist.model

import eu.kanade.tachiyomi.data.database.models.MangaSync
import eu.kanade.tachiyomi.data.mangasync.MangaSyncManager

data class ALManga(
        val id: Int,
        val title_romaji: String,
        val type: String,
        val total_chapters: Int) {

    fun toMangaSync() = MangaSync.create(MangaSyncManager.ANILIST).apply {
        remote_id = this@ALManga.id
        title = title_romaji
        total_chapters = this@ALManga.total_chapters
    }
}