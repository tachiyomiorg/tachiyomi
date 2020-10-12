package eu.kanade.tachiyomi.data.backup.models

import android.net.Uri

abstract class AbstractBackupManager {
    abstract fun createBackup(uri: Uri, flags: Int, isJob: Boolean): String?
}
