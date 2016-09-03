package eu.kanade.tachiyomi.data.mangasync.anilist.model

import eu.kanade.tachiyomi.data.database.models.MangaSync
import eu.kanade.tachiyomi.data.mangasync.MangaSyncManager
import eu.kanade.tachiyomi.data.mangasync.anilist.Anilist

data class ALUserManga(
        val id: Int,
        val list_status: String,
        val score_raw: Int,
        val chapters_read: Int,
        val manga: ALManga) {

    fun toMangaSync() = MangaSync.create(MangaSyncManager.ANILIST).apply {
        remote_id = manga.id
        status = getMangaSyncStatus()
        score = score_raw.toFloat()
        last_chapter_read = chapters_read
    }

    fun getMangaSyncStatus() = when (list_status) {
        "reading" -> Anilist.READING
        "completed" -> Anilist.COMPLETED
        "on-hold" -> Anilist.ON_HOLD
        "dropped" -> Anilist.DROPPED
        "plan to read" -> Anilist.PLAN_TO_READ
        else -> throw NotImplementedError("Unknown status")
    }
}