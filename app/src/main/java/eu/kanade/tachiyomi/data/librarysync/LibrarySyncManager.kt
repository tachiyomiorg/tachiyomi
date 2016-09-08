package eu.kanade.tachiyomi.data.librarysync

import android.content.Context
import android.webkit.URLUtil
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import rx.subjects.PublishSubject
import uy.kohesive.injekt.injectLazy
import xyz.nulldev.ts.sync.RxSyncClient
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class LibrarySyncManager(private val context: Context) {

    val LAST_LIBRARY_STATE_FILENAME = "last_library_state.json"

    private val preferences: PreferencesHelper by injectLazy()

    var syncClient: RxSyncClient? = null

    val syncAvailable: Boolean
        get() = syncClient != null

    var enabled: Boolean
        get() = preferences.enableLibrarySync().getOrDefault()
        set(value) = preferences.enableLibrarySync().set(value)

    var syncError: Int = R.string.sync_status_not_enabled

    var lastFailedSyncCount: Int
        get() = preferences.lastFailedLibrarySyncCount().getOrDefault()
        set(value) = preferences.lastFailedLibrarySyncCount().set(value)

    val syncAvailableSubject: PublishSubject<Boolean> = PublishSubject.create()

    init {
        //Observe preferences and update sync client when preferences change
        preferences.enableLibrarySync().asObservable().subscribe {
            updateSync(enableSync = it)
        }
        preferences.librarySyncEndpoint().asObservable().subscribe {
            updateSync(endpoint = it)
        }
    }

    /**
     * Ensure that an endpoint is valid
     */
    fun endpointValid(endpoint: String): Boolean {
        return !endpoint.trim().isEmpty() && URLUtil.isValidUrl(endpoint)
    }

    /**
     * Try to enable sync with the current conditions
     */
    private fun updateSync(enableSync: Boolean = preferences.enableLibrarySync().getOrDefault(),
                           endpoint: String = preferences.librarySyncEndpoint().getOrDefault()) {
        syncClient = null
        //Check the conditions for sync individually and log failed conditions, if all conditions are satisfied, the sync client will be initialized
        if (enableSync) {
            if (endpointValid(endpoint)) {
                initializeSyncClient(endpoint)
            } else {
                syncError = R.string.sync_status_error_invalid_endpoint
            }
        }
        //Notify sync manager listeners
        syncAvailableSubject.onNext(syncAvailable)
    }

    /**
     * Initialize the sync client
     */
    private fun initializeSyncClient(endpoint: String) {
        val builtEndpoint = if (!endpoint.endsWith("/")) endpoint + "/" else endpoint
        syncClient = RxSyncClient(builtEndpoint, true)
    }

    /**
     * Get library state at last sync
     */
    fun getLastLibraryState(): String {
        try {
            FileReader(getAbsoluteLastLibraryFile()).use { return it.readText() }
        } catch (e: Exception) {
            //Return an empty library if we have no last state
            return """{"mangas": [], "categories": []}""".trimMargin()
        }
    }

    /**
     * Save the last library state
     */
    fun saveLastLibraryState(state: String) {
        //First save to temp file
        val tmpFile = getAbsoluteTempLastLibraryFile()
        FileWriter(tmpFile, false).use {
            it.write(state)
        }
        //Move temp file to last library file state
        val lastLibraryFile = getAbsoluteLastLibraryFile()
        tmpFile.copyTo(lastLibraryFile, overwrite = true)
        //Delete temp file
        tmpFile.delete()
    }

    fun getAbsoluteLastLibraryFile(): File {
        return File(context.filesDir, LAST_LIBRARY_STATE_FILENAME)
    }

    private fun getAbsoluteTempLastLibraryFile(): File {
        return File(context.filesDir, "$LAST_LIBRARY_STATE_FILENAME.tmp")
    }
}