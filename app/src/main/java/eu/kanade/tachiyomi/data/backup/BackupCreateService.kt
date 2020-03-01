package eu.kanade.tachiyomi.data.backup

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.gson.JsonArray
import eu.kanade.tachiyomi.BuildConfig.APPLICATION_ID as ID
import eu.kanade.tachiyomi.data.database.models.Manga

/**
 * [IntentService] used to backup [Manga] information to [JsonArray]
 */
class BackupCreateService : IntentService(NAME) {

    companion object {
        // Name of class
        private const val NAME = "BackupCreateService"

        // Options for backup
        private const val EXTRA_FLAGS = "$ID.$NAME.EXTRA_FLAGS"

        // Filter options
        internal const val BACKUP_CATEGORY = 0x1
        internal const val BACKUP_CATEGORY_MASK = 0x1
        internal const val BACKUP_CHAPTER = 0x2
        internal const val BACKUP_CHAPTER_MASK = 0x2
        internal const val BACKUP_HISTORY = 0x4
        internal const val BACKUP_HISTORY_MASK = 0x4
        internal const val BACKUP_TRACK = 0x8
        internal const val BACKUP_TRACK_MASK = 0x8
        internal const val BACKUP_ALL = 0xF

        /**
         * Make a backup from library
         *
         * @param context context of application
         * @param uri path of Uri
         * @param flags determines what to backup
         */
        fun makeBackup(context: Context, uri: Uri, flags: Int) {
            val intent = Intent(context, BackupCreateService::class.java).apply {
                putExtra(BackupConst.EXTRA_URI, uri)
                putExtra(EXTRA_FLAGS, flags)
            }
            context.startService(intent)
        }
    }

    private val backupManager by lazy { BackupManager(this) }

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) return

        // Get values
        val uri = intent.getParcelableExtra<Uri>(BackupConst.EXTRA_URI)
        val flags = intent.getIntExtra(EXTRA_FLAGS, 0)
        // Create backup
        backupManager.createBackup(uri, flags, false)
    }
}
