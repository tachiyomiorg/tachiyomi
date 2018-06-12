package eu.kanade.tachiyomi.data.sync.protocol

import android.content.Context
import com.pushtorefresh.storio.sqlite.operations.put.PutResult
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.*
import eu.kanade.tachiyomi.data.sync.protocol.models.IntermediaryApplySyncReport
import eu.kanade.tachiyomi.data.sync.protocol.models.SyncReport
import eu.kanade.tachiyomi.data.sync.protocol.models.common.ChangedField
import eu.kanade.tachiyomi.data.sync.protocol.models.common.SyncRef
import eu.kanade.tachiyomi.data.sync.protocol.models.entities.*
import eu.kanade.tachiyomi.data.track.TrackManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class ReportApplier(val context: Context,
                    private val db: DatabaseHelper = Injekt.get()) {
    private val tracks: TrackManager by injectLazy()

    /**
     * Apply a sync report
     *
     * @param report The report to apply
     */
    fun apply(report: SyncReport) {
        //Enable optimizations
        report.tmpApply = IntermediaryApplySyncReport(report).apply { setup() }
        
        db.inTransaction {
            //Must be in order as some entities depend on previous entities to already be
            //in DB!
            applyManga(report)
            applyChapters(report)
            applyHistory(report)
            applyCategories(report)
            applyTracks(report)
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

            val id = dbManga.id ?: report.tmpApply.nextQueuedId()
            var changed = false

            it.favorite.applyIfNewer(report, id, UpdateTarget.Manga.favorite) {
                dbManga.favorite = it
                changed = true
            }
            it.viewer.applyIfNewer(report, id, UpdateTarget.Manga.viewer) {
                dbManga.viewer = it
                changed = true
            }
            it.chapterFlags.applyIfNewer(report, id, UpdateTarget.Manga.chapterFlags) {
                dbManga.chapter_flags = it
                changed = true
            }

            // Insert/update manga if changed
            if(changed || id < 0)
                db.insertManga(dbManga).executeAsBlocking().queueId(report, id)
        }
    }
    
    private fun applyChapters(report: SyncReport) {
        report.findEntities<SyncChapter>()
                .groupBy(SyncChapter::manga) //Process all chapters of same manga at the same time (less DB load)
                .forEach {
                    //Sometimes all chapters are already in DB, thus no need to resolve manga (so do not resolve manga immediately)
                    val dbManga by lazy(LazyThreadSafetyMode.NONE) {
                        val manga = it.key.resolve(report)
                        val source = manga.source.resolve(report)
                        db.getManga(manga.url, source.id).executeAsBlocking()
                    }
                    
                    it.value.forEach {
                        // Find existing chapter or create a new one
                        val dbChapter = db.getChapter(it.url).executeAsBlocking() ?:
                                Chapter.create().apply {
                                    url = it.url
                                    name = it.name
                                    chapter_number = it.chapterNum
                                    manga_id = dbManga?.id
                                    source_order = it.sourceOrder
                                }
                        
                        //Ensure manga chapter is in DB
                        if(dbChapter.manga_id != null) {
                            val id = dbChapter.id ?: report.tmpApply.nextQueuedId()
                            var changed = false
    
                            it.read.applyIfNewer(report, id, UpdateTarget.Chapter.read) {
                                dbChapter.read = it
                                changed = true
                            }
                            it.bookmark.applyIfNewer(report, id, UpdateTarget.Chapter.bookmark) {
                                dbChapter.bookmark = it
                                changed = true
                            }
                            it.lastPageRead.applyIfNewer(report, id, UpdateTarget.Chapter.lastPageRead) {
                                dbChapter.last_page_read = it
                                changed = true
                            }

                            // Insert/update chapter if changed
                            if(changed || id < 0)
                                db.insertChapter(dbChapter).executeAsBlocking().queueId(report, id)
                        }
                    }
                }
    }
    
    private fun applyHistory(report: SyncReport) {
        report.findEntities<SyncHistory>().forEach {
            val chapter = it.chapter.resolve(report)
            // Find existing history or create a new one
            val dbHistory = db.getHistoryByChapterUrl(chapter.url).executeAsBlocking() ?: run {
                val dbChapter = db.getChapter(chapter.url).executeAsBlocking()
                        ?: return@forEach // Chapter missing, do not sync this history
                History.create(dbChapter)
            }
            
            val id = dbHistory.id ?: report.tmpApply.nextQueuedId()
            var changed = false
            
            it.lastRead.applyIfNewer(report, id, UpdateTarget.History.lastRead) {
                dbHistory.last_read = it
                changed = true
            }

            // Insert/update history if changed
            if(changed || id < 0)
                db.insertHistory(dbHistory).executeAsBlocking().queueId(report, id)
        }
    }
    
    private fun applyCategories(report: SyncReport) {
        report.findEntities<SyncCategory>().forEach {
            val dbCategories = db.getCategories().executeAsBlocking()
            // Find existing category
            var dbCategory = dbCategories.find { dbCat ->
                it.name == dbCat.name
            }

            //Delete category if necessary
            if (it.deleted && dbCategory != null) {
                db.deleteCategory(dbCategory).executeAsBlocking()
                return@forEach
            }

            var changed = false

            // Perform renames
            if (it.oldName != null) {
                // Find category to rename
                val oldCat = dbCategories.find { dbCat ->
                    it.oldName!!.value == dbCat.name
                }

                if(oldCat != null) {
                    // No existing category with new name, rename old category to new name
                    if (dbCategory == null) {
                        oldCat.name = it.name
                        dbCategory = oldCat
                        changed = true
                    } else {
                        // Category with new name already exists, delete category with old name and
                        // move its manga into category with new name
                        val oldMangaCategories = db.getMangaCategoriesForCategory(oldCat).executeAsBlocking()
                        oldMangaCategories.map {
                            MangaCategory().apply {
                                manga_id = it.manga_id
                                category_id = dbCategory!!.id!!
                            }
                        }
                        db.deleteCategory(oldCat).executeAsBlocking()
                    }
                }
            }

            //No old category, create new one
            if(dbCategory == null)
                dbCategory = Category.create(it.name)

            //Apply other changes to category properties
            val id = dbCategory.id?.toLong() ?: report.tmpApply.nextQueuedId()

            it.flags.applyIfNewer(report, id, UpdateTarget.Category.flags) {
                dbCategory.flags = it
                changed = true
            }

            // Insert/update category if changed
            if(changed || id < 0) {
                val res = db.insertCategory(dbCategory).executeAsBlocking().queueId(report, id)
                res.insertedId()?.let {
                    dbCategory.id = it.toInt()
                }
            }

            /**
             * Bind a list of manga references to a category
             */
            fun List<SyncRef<SyncManga>>.toMangaCategories()
                    = mapNotNull {
                val manga = it.resolve(report)
                val source = manga.source.resolve(report)
                val dbManga = db.getManga(manga.url, source.id).executeAsBlocking()

                dbManga?.let {
                    MangaCategory.create(it, dbCategory)
                }
            }

            //Add/delete manga categories
            val addedMangaCategories = it.addedManga.toMangaCategories().filterNot {
                //Ensure DB does not have manga category
                db.hasMangaCategory(it.manga_id, it.category_id)
            }
            val removedMangaCategories = it.deletedManga.toMangaCategories()

            if(addedMangaCategories.isNotEmpty())
                db.insertMangasCategories(addedMangaCategories).executeAsBlocking()
            removedMangaCategories.forEach {
                db.deleteMangaCategory(it).executeAsBlocking()
            }
        }
    }

    private fun applyTracks(report: SyncReport) {
        val dbTracks = db.getTracks().executeAsBlocking()

        report.findEntities<SyncTrack>().forEach {
            val service = tracks.getService(it.sync_id) ?: return@forEach

            val manga = it.manga.resolve(report)
            val source = manga.source.resolve(report)
            val dbManga = db.getManga(manga.url, source.id).executeAsBlocking() ?: return@forEach

            //Delete track if necessary
            if (it.deleted) {
                db.deleteTrackForManga(dbManga, service).executeAsBlocking()
                return@forEach
            }

            // Find existing track or create a new one
            val dbTrack = dbTracks.find { dbTrack ->
                dbTrack.manga_id == dbManga.id && dbTrack.sync_id == it.sync_id
            } ?: Track.create(it.sync_id).apply {
                manga_id = dbManga.id ?: return@forEach
            }

            //Apply other changes to track properties
            val id = dbTrack.id ?: report.tmpApply.nextQueuedId()
            var changed = false

            it.media_id.applyIfNewer(report, id, UpdateTarget.Track.mediaId) {
                dbTrack.media_id = it
                changed = true
            }
            it.library_id.applyIfNewer(report, id, UpdateTarget.Track.libraryId) {
                dbTrack.library_id = it
                changed = true
            }
            it.title.applyIfNewer(report, id, UpdateTarget.Track.title) {
                dbTrack.title = it
                changed = true
            }
            it.last_chapter_read.applyIfNewer(report, id, UpdateTarget.Track.lastChapterRead) {
                dbTrack.last_chapter_read = it
                changed = true
            }
            it.total_chapters.applyIfNewer(report, id, UpdateTarget.Track.totalChapters) {
                dbTrack.total_chapters = it
                changed = true
            }
            it.score.applyIfNewer(report, id, UpdateTarget.Track.score) {
                dbTrack.score = it
                changed = true
            }
            it.status.applyIfNewer(report, id, UpdateTarget.Track.status) {
                dbTrack.status = it
                changed = true
            }
            it.tracking_url.applyIfNewer(report, id, UpdateTarget.Track.trackingUrl) {
                dbTrack.tracking_url = it
                changed = true
            }

            // Insert/update track if changed
            if(changed || id < 0)
                db.insertTrack(dbTrack).executeAsBlocking().queueId(report, id)
        }
    }

    /**
     * Queue a negative ID generated by [IntermediaryApplySyncReport.nextQueuedId] for later
     * mapping to a positive ID
     *
     * @receiver The insertion result to get the new inserted ID from
     * @param report The report to store the mapping in
     * @param origId The old negative ID
     * @return The receiver for convenience
     */
    private fun PutResult.queueId(report: SyncReport, origId: Long): PutResult {
        //Ensure original ID is negative
        if(origId < 0) {
            val insertedId = insertedId()
            //Ensure object was inserted
            if(insertedId != null) {
                report.tmpApply.queuedInsertedIds
                        .add(IntermediaryApplySyncReport.QueuedInsertedId(origId, insertedId))
            }
        }
        return this
    }

    /**
     * Apply the changes for a single property if the property change was newer
     * than the last time the property was changed in the DB or if the property never
     * existed
     *
     * @receiver The property change to apply. If `null` then the method will return immediately.
     * @param report The report where the property change is located in
     * @param id The id of the object in the database to apply the property change to
     *   If the object has not yet been inserted into the DB use a unique negative id,
     *   generated by [IntermediaryApplySyncReport.nextQueuedId]
     * @param field The field which was changed
     * @param exec Executed if the change should be applied
     */
    private fun <T> ChangedField<T>?.applyIfNewer(report: SyncReport,
                                                  id: Long,
                                                  field: UpdatableField,
                                                  exec: (T) -> Unit) {
        if(this != null
                && (id < 0
                        || db.getNewerEntryUpdate(id, field, this).executeAsBlocking() == null)) {
            exec(value)

            //Queue applied entry for timestamp correction later
            report.tmpApply.queuedTimestampEntries
                    .add(IntermediaryApplySyncReport.QueuedTimestampEntry(id, field, date))
        }
    }
}
