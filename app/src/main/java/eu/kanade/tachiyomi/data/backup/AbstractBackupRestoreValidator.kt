package eu.kanade.tachiyomi.data.backup

import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.SourceManager
import uy.kohesive.injekt.injectLazy

abstract class AbstractBackupRestoreValidator {
    protected val sourceManager: SourceManager by injectLazy()
    protected val trackManager: TrackManager by injectLazy()

    abstract fun validate(context: Context, uri: Uri): Results

    data class Results(val missingSources: List<String>, val missingTrackers: List<String>)
}
