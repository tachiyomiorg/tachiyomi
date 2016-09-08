package eu.kanade.tachiyomi.ui.library.sync

import android.os.Bundle
import com.google.gson.JsonParser
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.backup.BackupManager
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.librarysync.LibrarySyncManager
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import rx.Observable
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import timber.log.Timber
import uy.kohesive.injekt.injectLazy
import xyz.nulldev.ts.sync.model.SyncResult

class LibrarySyncPresenter(): BasePresenter<LibrarySyncDialogFragment>() {
    /**
     * Sync manager.
     */
    val syncManager: LibrarySyncManager by injectLazy()

    /**
     * Backup manager
     */
    val backupManager: BackupManager by lazy {
        BackupManager(db)
    }

    /**
     * Subject to notify the UI of sync progress updates
     */
    val syncProgressSubject: PublishSubject<LibrarySyncProgressEvent> = PublishSubject.create()

    /**
     * Database.
     */
    val db: DatabaseHelper by injectLazy()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        if(savedState == null) {
            runSync()
        }
    }

    /**
     * Run the sync operation
     */
    fun runSync() {
        //Ensure sync is available and enabled
        if (!syncManager.syncAvailable) {
            val error = if (!syncManager.enabled) {
                //Sync not enabled
                R.string.sync_status_not_enabled
            } else {
                //Sync not setup properly
                syncManager.syncError
            }
            syncProgressSubject.onNext(LibrarySyncProgressEvent.Error(context.getString(error)))
            return
        }
        val syncClient = syncManager.syncClient!!
        //Can't do IO on main thread!
        Observable.create<LibrarySyncProgressEvent> {
            val sub = it
            //Get libraries
            val oldLibrary = syncManager.getLastLibraryState()
            val newLibrary = backupManager.backupToJson(favoritesOnly = true).toString()

            //Show sync fail error dialog and log this failed sync attempt
            fun failSync(reason: String? = null) {
                sub.onNext(LibrarySyncProgressEvent.Error(reason ?: context.getString(R.string.sync_unknown_error)))
                //Log this failed sync
                syncManager.lastFailedSyncCount++
            }

            syncClient.syncLibrariesWithProgress(oldLibrary,
                    newLibrary).subscribe ({
                try {
                    if(it is SyncResult.Progress) {
                        //Update progress
                        sub.onNext(LibrarySyncProgressEvent.ProgressUpdated(it.status))
                    } else if (it is SyncResult.Success) {
                        //Report to user we are saving changes (it might take a while)
                        sub.onNext(LibrarySyncProgressEvent.ProgressUpdated(context.getString(R.string.saving_changes)))
                        //Everything in a transaction to ensure we don't delete the user's entire library in an error
                        db.inTransaction {
                            //Delete old mangas
                            val oldMangas = db.getMangas().executeAsBlocking()
                            if (oldMangas.size > 0) {
                                db.deleteMangas(oldMangas).executeAsBlocking()
                                db.deleteOldMangasCategories(oldMangas).executeAsBlocking()
                            }
                            //Delete old categories
                            val oldCategories = db.getCategories().executeAsBlocking()
                            if (oldCategories.size > 0) {
                                db.deleteCategories(oldCategories).executeAsBlocking()
                            }
                            //Write new library to DB
                            backupManager.restoreFromJson(JsonParser().parse(it.serializedLibrary).asJsonObject)
                            //Save new library state
                            syncManager.saveLastLibraryState(it.serializedLibrary)
                        }
                        //Sync ok, report conflicts
                        sub.onNext(LibrarySyncProgressEvent.Completed(it.conflicts))
                        //Sync was successful so reset last failed sync count
                        syncManager.lastFailedSyncCount = 0
                    } else if(it is SyncResult.Fail) {
                        //Sync failed
                        failSync(it.error)
                    }
                } catch (e: Exception) {
                    //Sync failed
                    Timber.e(e, "Sync failed!")
                    failSync()
                }
            }, {
                //Log this failed sync
                syncManager.lastFailedSyncCount++
                Timber.e(it, "Sync failed!")
                failSync(it.message)
            })
        }.subscribeOn(Schedulers.io()).subscribe(syncProgressSubject)
    }
}