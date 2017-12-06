package eu.kanade.tachiyomi.data.sync.protocol

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.*
import eu.kanade.tachiyomi.data.sync.protocol.models.SyncChapter
import eu.kanade.tachiyomi.data.sync.protocol.models.SyncHistory
import eu.kanade.tachiyomi.data.sync.protocol.models.SyncManga
import eu.kanade.tachiyomi.data.sync.protocol.models.SyncReport
import eu.kanade.tachiyomi.data.sync.protocol.models.common.ChangedField
import eu.kanade.tachiyomi.source.SourceManager
import uy.kohesive.injekt.injectLazy

class ReportApplier {
    private val db: DatabaseHelper by injectLazy()
    private val sources: SourceManager by injectLazy()

    fun apply(report: SyncReport) {
        //TODO Match sources
        db.inTransaction {
            //Must be in order as some entites depend on previous entities to already be
            //in DB!
            applyManga(report)
            applyChapters(report)
            applyHistory(report)
        }
    }

    private fun applyManga(report: SyncReport) {
        report.findEntities<SyncManga>().forEach {
            //Attempt to resolve previous update and only apply if new entry is newer
            val source = it.source.resolve(report)
            val dbManga = db.getManga(it.url, source.id).executeAsBlocking() ?: Manga.create(source.id).apply {
                url = it.url
                title = it.name
                thumbnail_url = it.thumbnailUrl

                this.initialized = false //New manga, fetch metadata next time we view it in UI
            }

            val id = dbManga.id

            it.favorite.applyIfNewer(id, UpdateTarget.Manga.favorite) { dbManga.favorite = it }
            it.viewer.applyIfNewer(id, UpdateTarget.Manga.viewer) { dbManga.viewer = it }
            it.chapterFlags.applyIfNewer(id, UpdateTarget.Manga.chapterFlags) { dbManga.chapter_flags = it }

            db.insertManga(dbManga).executeAsBlocking()
        }
    }

    private fun applyChapters(report: SyncReport) {
        report.findEntities<SyncChapter>().forEach {
            val dbChapter = db.getChapter(it.url).executeAsBlocking() ?: Chapter.create().apply {
                val manga = it.manga.resolve(report)
                val source = manga.source.resolve(report)
                val dbManga = db.getManga(manga.url, source.id).executeAsBlocking()!! //TODO

                url = it.url
                name = it.name
                chapter_number = it.chapterNum
                manga_id = dbManga.id
                source_order = it.sourceOrder
            }

            val id = dbChapter.id

            it.read.applyIfNewer(id, UpdateTarget.Chapter.read) { dbChapter.read = it }
            it.bookmark.applyIfNewer(id, UpdateTarget.Chapter.bookmark) { dbChapter.bookmark = it }
            it.lastPageRead.applyIfNewer(id, UpdateTarget.Chapter.lastPageRead) { dbChapter.last_page_read = it }

            db.insertChapter(dbChapter).executeAsBlocking()
        }
    }

    private fun applyHistory(report: SyncReport) {
        report.findEntities<SyncHistory>().forEach {
            val chapter = it.chapter.resolve(report)
            val dbHistory = db.getHistoryByChapterUrl(chapter.url).executeAsBlocking() ?: run {
                val dbChapter = db.getChapter(chapter.url).executeAsBlocking()!! //TODO
                History.create(dbChapter)
            }

            val id = dbHistory.id

            it.lastRead.applyIfNewer(id, UpdateTarget.History.lastRead) { dbHistory.last_read = it }

            db.insertHistory(dbHistory).executeAsBlocking()
        }
    }

    private fun <T> ChangedField<T>?.applyIfNewer(id: Long?,
                                                  field: UpdatableField,
                                                  exec: (T) -> Unit) {
        if(this != null
                && (id == null
                || db.getNewerEntryUpdates(id, field, this).executeAsBlocking().isEmpty())) {
            exec(value)
        }
    }
}
