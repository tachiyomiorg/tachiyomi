package eu.kanade.tachiyomi.data.sync.protocol

import android.content.Context
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.*
import eu.kanade.tachiyomi.data.sync.LibrarySyncManager
import eu.kanade.tachiyomi.data.sync.protocol.models.SyncReport
import eu.kanade.tachiyomi.data.sync.protocol.models.common.ChangedField
import eu.kanade.tachiyomi.data.sync.protocol.models.entities.*
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class ReportGenerator(val context: Context,
                      private val db: DatabaseHelper = Injekt.get(),
                      private val syncManager: LibrarySyncManager = Injekt.get()) {
    private val sources: SourceManager by injectLazy()

    /**
     * Generate a sync report of all changes between [from] and the current time
     *
     * @param currentDevice The current device's device ID
     * @param targetDevice The target's device ID
     * @param from The earliest date of changes that will be included in this report
     * @return The generated report
     */
    fun gen(currentDevice: String, targetDevice: String, from: Long, to: Long): SyncReport {
        val report = SyncReport()
        report.from = from
        report.to = to
        report.deviceId = currentDevice

        genManga(report)
        genChapters(report)
        genHistory(report)
        genCategories(targetDevice, report)
        genTracks(targetDevice, report)
        
        //Apply intermediary report changes to final report
        report.tmpGen.applyToReport()

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
    
    private fun genCategories(deviceId: String, report: SyncReport) {
        val flagsChanges = db.getEntryUpdatesForField(report, UpdateTarget.Category.flags).executeAsBlocking()
        flagsChanges.forEach {
            findOrGenCategory(it.updatedRow.toInt(), report)?.apply {
                first.flags = ChangedField(it.datetime, second.flags)
            }
        }
        
        //Find name changes, removed and added categories
        val categorySnapshots = syncManager.snapshots.readCategorySnapshots(deviceId)
        val categories = db.getCategories().executeAsBlocking()
        
        //Find added categories
        val added = categories.filter { category ->
            !categorySnapshots.any { it.matches(category) }
        }
        added.forEach {
            findOrGenCategory(it.id ?: return@forEach, report)
        }
        
        //Find deleted and name-changed categories
        val nameChanged = mutableListOf<Pair<String, Category>>()
        val deleted = mutableListOf<String>()
        categorySnapshots.forEach { snapshot ->
            val dbCategory = categories.find(snapshot::matches)
            if(dbCategory == null) {
                //Snapshot category no longer exists!
                deleted += snapshot.name
            } else if(dbCategory.name != snapshot.name) {
                //Category name changed!
                nameChanged += snapshot.name to dbCategory
            }
        }
        
        nameChanged.forEach {
            findOrGenCategory(it.second.id ?: return@forEach, report)?.apply {
                first.oldName = ChangedField(report.to, it.first)
            }
        }
        
        deleted.forEach {
            SyncCategory().apply {
                syncId = report.lastId++
                
                name = it
                
                this.deleted = true
                
                report.entities.add(this)
            }
        }
        
        //Gen added manga categories
        db.getAddedMangaCategories(deviceId).executeAsBlocking().forEach {
            val category = findOrGenCategory(it.category_id, report)
            val manga = findOrGenManga(it.manga_id, report)
            
            if(category != null && manga != null) {
                category.first.addedManga.add(manga.first.getRef())
            }
        }
        
        //Gen removed manga categories
        db.getDeletedMangaCategories(deviceId).executeAsBlocking().forEach {
            val category = findOrGenCategory(it.category_id, report)
            val manga = findOrGenManga(it.manga_id, report)
        
            if(category != null && manga != null) {
                category.first.deletedManga.add(manga.first.getRef())
            }
        }
    }
    
    private fun genTracks(deviceId: String, report: SyncReport) {
        val remoteIdChanges = db.getEntryUpdatesForField(report, UpdateTarget.Track.remoteId).executeAsBlocking()
        remoteIdChanges.forEach {
            findOrGenTrack(it.updatedRow, report)?.apply {
                first.remote_id = ChangedField(it.datetime, second.remote_id)
            }
        }
        
        val titleChanges = db.getEntryUpdatesForField(report, UpdateTarget.Track.title).executeAsBlocking()
        titleChanges.forEach {
            findOrGenTrack(it.updatedRow, report)?.apply {
                first.title = ChangedField(it.datetime, second.title)
            }
        }
        
        val lastChapterReadChanges = db.getEntryUpdatesForField(report, UpdateTarget.Track.lastChapterRead).executeAsBlocking()
        lastChapterReadChanges.forEach {
            findOrGenTrack(it.updatedRow, report)?.apply {
                first.last_chapter_read = ChangedField(it.datetime, second.last_chapter_read)
            }
        }
    
        val totalChaptersChanges = db.getEntryUpdatesForField(report, UpdateTarget.Track.totalChapters).executeAsBlocking()
        totalChaptersChanges.forEach {
            findOrGenTrack(it.updatedRow, report)?.apply {
                first.total_chapters = ChangedField(it.datetime, second.total_chapters)
            }
        }
        
        val scoreChanges = db.getEntryUpdatesForField(report, UpdateTarget.Track.score).executeAsBlocking()
        scoreChanges.forEach {
            findOrGenTrack(it.updatedRow, report)?.apply {
                first.score = ChangedField(it.datetime, second.score)
            }
        }
        
        val statusChanges = db.getEntryUpdatesForField(report, UpdateTarget.Track.status).executeAsBlocking()
        statusChanges.forEach {
            findOrGenTrack(it.updatedRow, report)?.apply {
                first.status = ChangedField(it.datetime, second.status)
            }
        }
        
        //Find added/removed tracks
        val trackSnapshots = syncManager.snapshots.readTrackSnapshots(deviceId)
        val tracks = db.getTracks().executeAsBlocking()
    
        //Find added tracks
        val added = tracks.filter { track ->
            !trackSnapshots.any { it.matches(track) }
        }
        added.forEach {
            findOrGenTrack(it.id ?: return@forEach, report)
        }
        
        //Find removed tracks
        val deleted = trackSnapshots.filter { snapshot ->
            !tracks.any(snapshot::matches)
        }
        deleted.forEach {
            val manga = findOrGenManga(it.mangaId, report)?.first ?: return@forEach
            SyncTrack().apply {
                this.syncId = report.lastId++
    
                this.manga = manga.getRef()
                this.sync_id = it.syncId
    
                this.deleted = true
        
                report.entities.add(this)
            }
        }
    }
    
    private fun findOrGenSource(id: Long, report: SyncReport): Pair<SyncSource, Source>? {
        val source = sources.get(id) ?: return null
        
        return Pair(report.tmpGen.sources.find { it.id == id } ?: SyncSource().apply {
            this.syncId = report.lastId++
            
            this.id = id
            this.name = source.name
            
            report.tmpGen.sources.add(this)
        }, source)
    }
    
    private fun findOrGenManga(id: Long, report: SyncReport): Pair<SyncManga, Manga>? {
        val manga = db.getManga(id).executeAsBlocking() ?: return null
        val source = findOrGenSource(manga.source, report)?.first ?: return null
        
        return Pair(report.tmpGen.mangas.find {
            it.source.targetId == source.syncId && it.url == manga.url
        } ?: SyncManga().apply {
            this.syncId = report.lastId++
            
            this.source = source.getRef()
            this.url = manga.url
            this.name = manga.title
            this.thumbnailUrl = manga.thumbnail_url
            
            report.tmpGen.mangas.add(this)
        }, manga)
    }
    
    private fun findOrGenChapter(id: Long, report: SyncReport): Pair<SyncChapter, Chapter>? {
        val chapter = db.getChapter(id).executeAsBlocking() ?: return null
        val manga = findOrGenManga(chapter.manga_id ?: return null, report)?.first ?: return null
        
        return Pair(report.tmpGen.chapters.find {
            it.manga.targetId == manga.syncId && it.url == chapter.url
        } ?: SyncChapter().apply {
            this.syncId = report.lastId++
            
            this.manga = manga.getRef()
            this.chapterNum = chapter.chapter_number
            this.sourceOrder = chapter.source_order
            this.url = chapter.url
            this.name = chapter.name
            
            report.tmpGen.chapters.add(this)
        }, chapter)
    }
    
    private fun findOrGenHistory(id: Long, report: SyncReport): Pair<SyncHistory, History>? {
        val history = db.getHistory(id).executeAsBlocking() ?: return null
        val chapter = findOrGenChapter(history.chapter_id, report)?.first ?: return null
        
        return Pair(report.tmpGen.histories.find {
            it.chapter.targetId == chapter.syncId
        } ?: SyncHistory().apply {
            this.syncId = report.lastId++
            
            this.chapter = chapter.getRef()
            
            report.tmpGen.histories.add(this)
        }, history)
    }
    
    private fun findOrGenCategory(id: Int, report: SyncReport): Pair<SyncCategory, Category>? {
        val category = db.getCategory(id).executeAsBlocking() ?: return null
        
        return Pair(report.tmpGen.categories.find {
            it.name == category.name
        } ?: SyncCategory().apply {
            this.syncId = report.lastId++
            
            this.name = category.name
            
            report.tmpGen.categories.add(this)
        }, category)
    }
    
    private fun findOrGenTrack(id: Long, report: SyncReport): Pair<SyncTrack, Track>? {
        val track = db.getTrack(id).executeAsBlocking() ?: return null
        val manga = findOrGenManga(track.manga_id, report)?.first ?: return null
        
        return Pair(report.tmpGen.tracks.find {
            it.manga.targetId == manga.syncId && it.sync_id == track.sync_id
        } ?: SyncTrack().apply {
            this.syncId = report.lastId++
            
            this.manga = manga.getRef()
            this.sync_id = track.sync_id
            
            report.tmpGen.tracks.add(this)
        }, track)
    }
}