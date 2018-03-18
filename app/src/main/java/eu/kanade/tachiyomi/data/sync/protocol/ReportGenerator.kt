package eu.kanade.tachiyomi.data.sync.protocol

import android.content.Context
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.*
import eu.kanade.tachiyomi.data.sync.LibrarySyncManager
import eu.kanade.tachiyomi.data.sync.protocol.models.IntermediaryGenSyncReport
import eu.kanade.tachiyomi.data.sync.protocol.models.SyncReport
import eu.kanade.tachiyomi.data.sync.protocol.models.common.ChangedField
import eu.kanade.tachiyomi.data.sync.protocol.models.common.SyncEntity
import eu.kanade.tachiyomi.data.sync.protocol.models.entities.*
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

private typealias EntityFinder<A, B> = (EntryUpdate) -> Pair<A, B>?

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
        report.tmpGen = IntermediaryGenSyncReport(report)

        // Actually generate report
        genManga(report)
        genChapters(report)
        genHistory(report)
        genCategories(targetDevice, report)
        genTracks(targetDevice, report)
        
        // Apply intermediary report changes to final report
        report.tmpGen.applyToReport()

        return report
    }

    private fun genManga(report: SyncReport) {
        // Lambda used to convert between a database row -> a sync entity
        val entityFinder: EntityFinder<SyncManga, Manga>
                = { findOrGenManga(it.updatedRow, report) }

        // Copy each change in the database -> sync entities

        genForEntryUpdates(report, UpdateTarget.Manga.favorite, entityFinder) {
            first.favorite = ChangedField(it.datetime, second.favorite)
        }

        genForEntryUpdates(report, UpdateTarget.Manga.viewer, entityFinder) {
            first.viewer = ChangedField(it.datetime, second.viewer)
        }

        genForEntryUpdates(report, UpdateTarget.Manga.chapterFlags, entityFinder) {
            first.chapterFlags = ChangedField(it.datetime, second.chapter_flags)
        }
    }

    private fun genChapters(report: SyncReport) {
        // Lambda used to convert between a database row -> a sync entity
        val entityFinder: EntityFinder<SyncChapter, Chapter>
                = { findOrGenChapter(it.updatedRow, report) }

        // Copy each change in the database -> sync entities

        genForEntryUpdates(report, UpdateTarget.Chapter.read, entityFinder) {
            first.read = ChangedField(it.datetime, second.read)
        }

        genForEntryUpdates(report, UpdateTarget.Chapter.bookmark, entityFinder) {
            first.bookmark = ChangedField(it.datetime, second.bookmark)
        }

        genForEntryUpdates(report, UpdateTarget.Chapter.lastPageRead, entityFinder) {
            first.lastPageRead = ChangedField(it.datetime, second.last_page_read)
        }
    }

    private fun genHistory(report: SyncReport) {
        // Lambda used to convert between a database row -> a sync entity
        val entityFinder: EntityFinder<SyncHistory, History>
                = { findOrGenHistory(it.updatedRow, report) }

        // Copy each change in the database -> sync entities

        genForEntryUpdates(report, UpdateTarget.History.lastRead, entityFinder) {
            first.lastRead = ChangedField(it.datetime, second.last_read)
        }
    }
    
    private fun genCategories(deviceId: String, report: SyncReport) {
        // Lambda used to convert between a database row -> a sync entity
        val entityFinder: EntityFinder<SyncCategory, Category>
                = { findOrGenCategory(it.updatedRow.toInt(), report) }

        // Copy each change in the database -> sync entities

        genForEntryUpdates(report, UpdateTarget.Category.flags, entityFinder) {
            first.flags = ChangedField(it.datetime, second.flags)
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

        // Save name changes to sync entity
        nameChanged.forEach {
            findOrGenCategory(it.second.id ?: return@forEach, report)?.apply {
                first.oldName = ChangedField(report.to, it.first)
            }
        }
        
        deleted.forEach {
            // Deleted categories are not generated using the normal methods as their
            // database rows no longer exist
            SyncCategory().apply {
                syncId = report.lastId++
                
                name = it
                
                this.deleted = true

                // Do not add to intermediary data source as this entity will never be searched for
                // (so we don't need the search optimizations)
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
        // Lambda used to convert between a database row -> a sync entity
        val entityFinder: EntityFinder<SyncTrack, Track>
                = { findOrGenTrack(it.updatedRow, report) }

        // Copy each change in the database -> sync entities

        genForEntryUpdates(report, UpdateTarget.Track.remoteId, entityFinder) {
            first.remote_id = ChangedField(it.datetime, second.remote_id)
        }
        
        genForEntryUpdates(report, UpdateTarget.Track.title, entityFinder) {
            first.title = ChangedField(it.datetime, second.title)
        }
        
        genForEntryUpdates(report, UpdateTarget.Track.lastChapterRead, entityFinder) {
            first.last_chapter_read = ChangedField(it.datetime, second.last_chapter_read)
        }
    
        genForEntryUpdates(report, UpdateTarget.Track.totalChapters, entityFinder) {
            first.total_chapters = ChangedField(it.datetime, second.total_chapters)
        }
        
        genForEntryUpdates(report, UpdateTarget.Track.score, entityFinder) {
            first.score = ChangedField(it.datetime, second.score)
        }
        
        genForEntryUpdates(report, UpdateTarget.Track.status, entityFinder) {
            first.status = ChangedField(it.datetime, second.status)
        }

        genForEntryUpdates(report, UpdateTarget.Track.trackingUrl, entityFinder) {
            first.tracking_url = ChangedField(it.datetime, second.tracking_url)
        }

        //Find added/removed tracks
        // Get tracks from snapshot and db
        val trackSnapshots = syncManager.snapshots.readTrackSnapshots(deviceId)
        val tracks = db.getTracks().executeAsBlocking()
    
        //Find added tracks (tracks in db that are not in snapshot)
        val added = tracks.filter { track ->
            !trackSnapshots.any { it.matches(track) }
        }
        added.forEach {
            findOrGenTrack(it.id ?: return@forEach, report)
        }
        
        //Find removed tracks (tracks in snapshot that are not db)
        val deleted = trackSnapshots.filter { snapshot ->
            !tracks.any(snapshot::matches)
        }
        deleted.forEach {
            // Deleted track snapshots are not generated using the normal methods as their
            // database rows no longer exist
            val manga = findOrGenManga(it.mangaId, report)?.first ?: return@forEach
            SyncTrack().apply {
                this.syncId = report.lastId++
    
                this.manga = manga.getRef()
                this.sync_id = it.syncId
    
                this.deleted = true

                // Do not add to intermediary data source as this entity will never be searched for
                // (so we don't need the search optimizations)
                report.entities.add(this)
            }
        }
    }

    /**
     * Find an existing sync entity or create a new sync entity that will represent a particular
     * database row in the sync report
     *
     * @param id The id of the database row to create the entity for
     * @param report The report
     * @returns A pair of the sync entity along with the database model corresponding to the provided row id
     */
    private fun findOrGenSource(id: Long, report: SyncReport): Pair<SyncSource, Source>? {
        // Find database model for this row and find dependent sync entities
        val source = sources.get(id) ?: return null

        // Find existing entity or generate new one if no existing entity
        return Pair(report.tmpGen.sources.find { it.id == id } ?: SyncSource().apply {
            this.syncId = report.lastId++
            
            this.id = id
            this.name = source.name
            
            report.tmpGen.sources.add(this)
        }, source)
    }

    /**
     * Find an existing sync entity or create a new sync entity that will represent a particular
     * database row in the sync report
     *
     * @param id The id of the database row to create the entity for
     * @param report The report
     * @returns A pair of the sync entity along with the database model corresponding to the provided row id
     */
    private fun findOrGenManga(id: Long, report: SyncReport): Pair<SyncManga, Manga>? {
        // Find database model for this row and find dependent sync entities
        val manga = db.getManga(id).executeAsBlocking() ?: return null
        val source = findOrGenSource(manga.source, report)?.first ?: return null

        // Find existing entity or generate new one if no existing entity
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

    /**
     * Find an existing sync entity or create a new sync entity that will represent a particular
     * database row in the sync report
     *
     * @param id The id of the database row to create the entity for
     * @param report The report
     * @returns A pair of the sync entity along with the database model corresponding to the provided row id
     */
    private fun findOrGenChapter(id: Long, report: SyncReport): Pair<SyncChapter, Chapter>? {
        // Find database model for this row and find dependent sync entities
        val chapter = db.getChapter(id).executeAsBlocking() ?: return null
        val manga = findOrGenManga(chapter.manga_id ?: return null, report)?.first ?: return null

        // Find existing entity or generate new one if no existing entity
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

    /**
     * Find an existing sync entity or create a new sync entity that will represent a particular
     * database row in the sync report
     *
     * @param id The id of the database row to create the entity for
     * @param report The report
     * @returns A pair of the sync entity along with the database model corresponding to the provided row id
     */
    private fun findOrGenHistory(id: Long, report: SyncReport): Pair<SyncHistory, History>? {
        // Find database model for this row and find dependent sync entities
        val history = db.getHistory(id).executeAsBlocking() ?: return null
        val chapter = findOrGenChapter(history.chapter_id, report)?.first ?: return null

        // Find existing entity or generate new one if no existing entity
        return Pair(report.tmpGen.histories.find {
            it.chapter.targetId == chapter.syncId
        } ?: SyncHistory().apply {
            this.syncId = report.lastId++
            
            this.chapter = chapter.getRef()
            
            report.tmpGen.histories.add(this)
        }, history)
    }

    /**
     * Find an existing sync entity or create a new sync entity that will represent a particular
     * database row in the sync report
     *
     * @param id The id of the database row to create the entity for
     * @param report The report
     * @returns A pair of the sync entity along with the database model corresponding to the provided row id
     */
    private fun findOrGenCategory(id: Int, report: SyncReport): Pair<SyncCategory, Category>? {
        // Find database model for this row and find dependent sync entities
        val category = db.getCategory(id).executeAsBlocking() ?: return null

        // Find existing entity or generate new one if no existing entity
        return Pair(report.tmpGen.categories.find {
            it.name == category.name
        } ?: SyncCategory().apply {
            this.syncId = report.lastId++
            
            this.name = category.name
            
            report.tmpGen.categories.add(this)
        }, category)
    }

    /**
     * Find an existing sync entity or create a new sync entity that will represent a particular
     * database row in the sync report
     *
     * @param id The id of the database row to create the entity for
     * @param report The report
     * @returns A pair of the sync entity along with the database model corresponding to the provided row id
     */
    private fun findOrGenTrack(id: Long, report: SyncReport): Pair<SyncTrack, Track>? {
        // Find database model for this row and find dependent sync entities
        val track = db.getTrack(id).executeAsBlocking() ?: return null
        val manga = findOrGenManga(track.manga_id, report)?.first ?: return null

        // Find existing entity or generate new one if no existing entity
        return Pair(report.tmpGen.tracks.find {
            it.manga.targetId == manga.syncId && it.sync_id == track.sync_id
        } ?: SyncTrack().apply {
            this.syncId = report.lastId++
            
            this.manga = manga.getRef()
            this.sync_id = track.sync_id
            
            report.tmpGen.tracks.add(this)
        }, track)
    }

    /**
     * Find all changes in the DB for a particular field, get sync entities
     * for the rows where the changes occurred and run a lambda to apply the changes
     * to the sync entity
     *
     * @param report The report
     * @param target The field to find the changes for
     * @param entityFinder A lambda used to obtain a sync entity from a database row
     * @param exec A lambda that will apply the a change to a sync entity
     */
    private fun <A : SyncEntity<A>, B> genForEntryUpdates(report: SyncReport,
                                          target: UpdatableField,
                                          entityFinder: EntityFinder<A, B>,
                                          exec: Pair<A, B>.(EntryUpdate) -> Unit) {
        // Find DB changes
        val changes = db.getEntryUpdatesForField(report, target).executeAsBlocking()
        changes.forEach {
            // Find sync entity for DB row
            val res = entityFinder(it)

            // Apply change to sync entity
            if(res != null)
                exec(res, it)
        }
    }

}
