package eu.kanade.tachiyomi.ui.library.sync

import android.app.Activity
import com.afollestad.materialdialogs.MaterialDialog
import com.google.gson.JsonParser
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.backup.BackupManager
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.librarysync.LibrarySyncManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import uy.kohesive.injekt.injectLazy
import kotlin.concurrent.thread

class LibrarySyncDialog(val activity: Activity) {
    val sync: LibrarySyncManager by injectLazy()

    val prefs: PreferencesHelper by injectLazy()

    val db: DatabaseHelper by injectLazy()

    val backup: BackupManager by lazy {
        BackupManager(db)
    }

    lateinit var materialDialog: MaterialDialog

    /**
     * Create a dialog that will display sync progress
     */
    fun createDialog(): MaterialDialog {
        materialDialog = MaterialDialog.Builder(activity)
                .progress(true, 0)
                .title(R.string.library_sync_dialog_title)
                .content(R.string.uploading_library)
                .cancelable(false)
                .build()
        return materialDialog
    }

    /**
     * Actually run the sync
     *
     * Make sure you have called createDialog() before calling this method
     */
    fun runSync() {
        //Ensure sync is available and enabled
        if (!sync.syncAvailable) {
            materialDialog.dismiss()
            if(!sync.enabled) {
                //Sync not enabled
                reportError(activity.getString(R.string.sync_status_not_enabled))
            } else {
                //Sync not setup properly
                reportError(activity.getString(sync.syncError))
            }
            return
        }
        val syncClient = sync.syncClient!!
        //New thread, can't do IO on main thread
        thread {
            //Get libraries
            val oldLibrary = sync.getLastLibraryState()
            val newLibrary = backup.backupToJson(favoritesOnly = prefs.syncFavoritesOnly().getOrDefault()).toString()
            syncClient.syncLibrariesWithProgress(oldLibrary,
                    newLibrary, {
                //Sync is complete (successful or not)
                try {
                    if (it.isSuccessful) {
                        //Report to user we are saving changes (it might take a while)
                        activity.runOnUiThread { materialDialog.setContent(R.string.saving_changes) }
                        //Everything in a transaction to ensure we don't delete the user's entire library in an error
                        db.inTransaction {
                            //Delete old mangas
                            val oldMangas = db.getMangas().executeAsBlocking()
                            if(oldMangas.size > 0) {
                                db.deleteMangas(oldMangas).executeAsBlocking()
                                db.deleteOldMangasCategories(oldMangas).executeAsBlocking()
                            }
                            //Delete old categories
                            val oldCategories = db.getCategories().executeAsBlocking()
                            if(oldCategories.size > 0 ) {
                                db.deleteCategories(oldCategories).executeAsBlocking()
                            }
                            //Write new library to DB
                            backup.restoreFromJson(JsonParser().parse(it.serializedLibrary).asJsonObject)
                            //Save new library state
                            sync.saveLastLibraryState(it.serializedLibrary)
                        }
                        //Report any conflicts
                        if (it.conflicts.size > 0) {
                            activity.runOnUiThread { reportConflicts(it.conflicts) }
                        }
                        //Sync was successful so reset last failed sync count
                        sync.lastFailedSyncCount = 0
                    } else {
                        //Invoke exception error handler to launch error dialog? Better way to do this?
                        throw IllegalStateException("Sync failed on server!")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    //Show error dialog
                    activity.runOnUiThread {
                        reportError(it.error ?: activity.getString(R.string.sync_unknown_error))
                    }
                    //Log this failed sync
                    sync.lastFailedSyncCount++
                }
                //Ensure dialog is dismissed
                materialDialog.dismiss()
            }, { complete, details ->
                //Update progress
                activity.runOnUiThread {
                    //Dismiss dialog/set progress on progress update
                    if (complete)
                        materialDialog.dismiss()
                    else
                        materialDialog.setContent(details)
                }
            })
        }
    }

    /**
     * Report a conflict
     *
     * @param conflicts A list of conflicts
     */
    private fun reportConflicts(conflicts: List<String>) {
        basicDialog(R.string.sync_conflicts,
                conflicts.joinToString(separator = "\n"))
    }

    /**
     * Report an error
     *
     * @param error The error to report
     */
    private fun reportError(error: String) {
        basicDialog(R.string.sync_error, error)
    }

    /**
     * Show a dialog with a String resource title, a String body and an OK button
     *
     * @param title The String resource title
     * @param content The content
     */
    private fun basicDialog(title: Int, content: String) {
        MaterialDialog.Builder(activity)
                .title(title)
                .content(content)
                .cancelable(true)
                .positiveText(android.R.string.ok)
                .show()
    }
}