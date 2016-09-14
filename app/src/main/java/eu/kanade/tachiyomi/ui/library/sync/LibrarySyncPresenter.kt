package eu.kanade.tachiyomi.ui.library.sync

import android.os.Bundle
import com.google.gson.JsonParser
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.backup.BackupManager
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.librarysync.LibrarySyncManager
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import rx.Observable
import rx.functions.Func1
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
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
        //Get library
        getLibraryObservable().concatMap {
            //Sync library
            syncClient.syncLibrariesWithProgress(it.first,
                    it.second).concatMap<LibrarySyncProgressEvent> (Func1<SyncResult, Observable<LibrarySyncProgressEvent>> { result ->
                when(result) {
                    //Update progress
                    is SyncResult.Progress -> Observable.just(LibrarySyncProgressEvent.ProgressUpdated(result.status))
                    is SyncResult.Success -> {
                        //Report to user we are saving changes (it might take a while)
                        val savedObservable = Observable.just(LibrarySyncProgressEvent.ProgressUpdated(context.getString(R.string.saving_changes)))
                        val completedObservable = Observable.fromCallable {
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
                                backupManager.restoreFromJson(JsonParser().parse(result.serializedLibrary).asJsonObject)
                                //Save new library state
                                syncManager.saveLastLibraryState(result.serializedLibrary)
                            }
                            //Sync was successful so reset last failed sync count
                            syncManager.lastFailedSyncCount = 0
                            //Sync ok, report conflicts
                            LibrarySyncProgressEvent.Completed(result.conflicts)
                        }
                        Observable.merge(savedObservable, completedObservable)
                    }
                    is SyncResult.Fail -> {
                        //Log this failed sync
                        syncManager.lastFailedSyncCount++
                        Observable.just<LibrarySyncProgressEvent>(LibrarySyncProgressEvent.Error(result.error ?: context.getString(R.string.sync_unknown_error)))
                    }
                }
            })
        }.subscribeOn(Schedulers.io()).subscribe(syncProgressSubject)
    }

    /**
     * Get the library states
     *
     * @returns Pair<OLD, NEW>
     */
    fun getLibraryObservable():Observable<Pair<String, String>> {
        return Observable.fromCallable {
            //Get libraries
            val oldLibrary = syncManager.getLastLibraryState()
            val newLibrary = backupManager.backupToJson(favoritesOnly = true).toString()
            Pair(oldLibrary, newLibrary)
        }
    }
}