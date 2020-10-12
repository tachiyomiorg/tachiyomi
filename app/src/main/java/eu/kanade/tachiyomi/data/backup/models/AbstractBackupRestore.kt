package eu.kanade.tachiyomi.data.backup.models

import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.data.backup.BackupNotifier
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import kotlinx.coroutines.Job
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

abstract class AbstractBackupRestore(protected val context: Context, protected val notifier: BackupNotifier) {
    protected val db: DatabaseHelper by injectLazy()

    protected val trackManager: TrackManager by injectLazy()

    var job: Job? = null

    /**
     * The progress of a backup restore
     */
    protected var restoreProgress = 0

    /**
     * Amount of manga in Json file (needed for restore)
     */
    protected var restoreAmount = 0

    /**
     * Mapping of source ID to source name from backup data
     */
    protected var sourceMapping: Map<Long, String> = emptyMap()

    /**
     * List containing errors
     */
    protected val errors = mutableListOf<Pair<Date, String>>()

    abstract fun restoreBackup(uri: Uri): Boolean

    /**
     * Write errors to error log
     */
    fun writeErrorLog(): File {
        try {
            if (errors.isNotEmpty()) {
                val destFile = File(context.externalCacheDir, "tachiyomi_restore.txt")
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

                destFile.bufferedWriter().use { out ->
                    errors.forEach { (date, message) ->
                        out.write("[${sdf.format(date)}] $message\n")
                    }
                }
                return destFile
            }
        } catch (e: Exception) {
            // Empty
        }
        return File("")
    }
}
