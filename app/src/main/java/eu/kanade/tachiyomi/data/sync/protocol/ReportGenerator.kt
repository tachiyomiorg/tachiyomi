package eu.kanade.tachiyomi.data.sync.protocol

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.UpdateTarget
import eu.kanade.tachiyomi.data.sync.protocol.models.*
import eu.kanade.tachiyomi.data.sync.protocol.models.common.ChangedField
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import uy.kohesive.injekt.injectLazy

class ReportGenerator {
    private val db: DatabaseHelper by injectLazy()
    private val sources: SourceManager by injectLazy()

    fun gen(from: Long): SyncReport {
        val report = SyncReport()
        report.from = from
        report.to = System.currentTimeMillis()

        genManga(report)
        genChapters(report)
        genHistory(report)

        return report
    }

    private fun genManga(report: SyncReport) {
        val favChanges = db.getEntryUpdatesForField(report, UpdateTarget.Manga.favorite).executeAsBlocking()
        favChanges.forEach {
            findOrGenManga(it.updatedRow, report)?.apply {
                first.favorite = ChangedField(it.datetime, second.favorite)
            }
        }

        val viewerChanges = db.getEntryUpdatesForField(report, UpdateTarget.Manga.viewer).executeAsBlocking()
        viewerChanges.forEach {
            findOrGenManga(it.updatedRow, report)?.apply {
                first.viewer = ChangedField(it.datetime, second.viewer)
            }
        }

        val chapterFlagChanges = db.getEntryUpdatesForField(report, UpdateTarget.Manga.chapterFlags).executeAsBlocking()
        chapterFlagChanges.forEach {
            findOrGenManga(it.updatedRow, report)?.apply {
                first.chapterFlags = ChangedField(it.datetime, second.chapter_flags)
            }
        }
    }

    private fun genChapters(report: SyncReport) {
        val readChanges = db.getEntryUpdatesForField(report, UpdateTarget.Chapter.read).executeAsBlocking()
        readChanges.forEach {
            findOrGenChapter(it.updatedRow, report)?.apply {
                first.read = ChangedField(it.datetime, second.read)
            }
        }

        val bookmarkChanges = db.getEntryUpdatesForField(report, UpdateTarget.Chapter.bookmark).executeAsBlocking()
        bookmarkChanges.forEach {
            findOrGenChapter(it.updatedRow, report)?.apply {
                first.bookmark = ChangedField(it.datetime, second.bookmark)
            }
        }

        val lastPageReadChanges = db.getEntryUpdatesForField(report, UpdateTarget.Chapter.lastPageRead).executeAsBlocking()
        lastPageReadChanges.forEach {
            findOrGenChapter(it.updatedRow, report)?.apply {
                first.lastPageRead = ChangedField(it.datetime, second.last_page_read)
            }
        }
    }

    private fun genHistory(report: SyncReport) {
        val lastReadChanges = db.getEntryUpdatesForField(report, UpdateTarget.History.lastRead).executeAsBlocking()
        lastReadChanges.forEach {
            findOrGenHistory(it.updatedRow, report)?.apply {
                first.lastRead = ChangedField(it.datetime, second.last_read)
            }
        }
    }

    private fun findOrGenSource(id: Long, report: SyncReport): Pair<SyncSource, Source>? {
        val source = sources.get(id) ?: return null

        return Pair(report.findEntity { it.id == id } ?: SyncSource().apply {
            this.syncId = report.lastId++

            this.id = id
            this.name = source.name

            report.entities.add(this)
        }, source)
    }

    private fun findOrGenManga(id: Long, report: SyncReport): Pair<SyncManga, Manga>? {
        val manga = db.getManga(id).executeAsBlocking() ?: return null
        val source = findOrGenSource(manga.source, report)!!.first

        return Pair(report.findEntity {
            it.source.targetId == source.syncId && it.url == manga.url
        } ?: SyncManga().apply {
            this.syncId = report.lastId++

            this.source = source.getRef()
            this.url = manga.url
            this.name = manga.title
            this.thumbnailUrl = manga.thumbnail_url

            report.entities.add(this)
        }, manga)
    }

    private fun findOrGenChapter(id: Long, report: SyncReport): Pair<SyncChapter, Chapter>? {
        val chapter = db.getChapter(id).executeAsBlocking() ?: return null
        val manga = findOrGenManga(chapter.manga_id!!, report)!!.first

        return Pair(report.findEntity {
            it.manga.targetId == manga.syncId && it.url == chapter.url
        } ?: SyncChapter().apply {
            this.syncId = report.lastId++

            this.manga = manga.getRef()
            this.chapterNum = chapter.chapter_number
            this.sourceOrder = chapter.source_order
            this.url = chapter.url
            this.name = chapter.name

            report.entities.add(this)
        }, chapter)
    }

    private fun findOrGenHistory(id: Long, report: SyncReport): Pair<SyncHistory, History>? {
        val history = db.getHistory(id).executeAsBlocking() ?: return null
        val chapter = findOrGenChapter(history.chapter_id, report)!!.first

        return Pair(report.findEntity {
            it.chapter.targetId == chapter.syncId
        } ?: SyncHistory().apply {
            this.syncId = report.lastId++

            this.chapter = chapter.getRef()

            report.entities.add(this)
        }, history)
    }
}