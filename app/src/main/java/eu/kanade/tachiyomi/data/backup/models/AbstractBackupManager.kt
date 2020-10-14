package eu.kanade.tachiyomi.data.backup.models

import android.net.Uri
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.SourceManager
import uy.kohesive.injekt.injectLazy

abstract class AbstractBackupManager {
    internal val databaseHelper: DatabaseHelper by injectLazy()
    internal val sourceManager: SourceManager by injectLazy()
    internal val trackManager: TrackManager by injectLazy()
    protected val preferences: PreferencesHelper by injectLazy()

    abstract fun createBackup(uri: Uri, flags: Int, isJob: Boolean): String?
}
