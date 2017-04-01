package eu.kanade.tachiyomi.data.backup

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.github.salomonbrys.kotson.toJson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.backup.models.JSON
import eu.kanade.tachiyomi.data.backup.models.JSON.AMOUNT
import eu.kanade.tachiyomi.data.backup.models.JSON.CATEGORIES
import eu.kanade.tachiyomi.data.backup.models.JSON.CURRENT_VERSION
import eu.kanade.tachiyomi.data.backup.models.JSON.INFORMATION
import eu.kanade.tachiyomi.data.backup.models.JSON.MANGAS
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.ui.setting.SettingsBackupFragment
import eu.kanade.tachiyomi.util.sendLocalBroadcast
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import eu.kanade.tachiyomi.BuildConfig.APPLICATION_ID as ID

/**
 * [IntentService] used to backup [Manga] information to [JsonArray]
 */
class BackupCreateService : IntentService(NAME) {

    companion object {
        // Name of class
        private const val NAME = "BackupCreateService"

        // Uri as string
        private const val EXTRA_URI = "$ID.$NAME.EXTRA_URI"
        // Backup called from job
        private const val EXTRA_IS_JOB = "$ID.$NAME.EXTRA_IS_JOB"
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
         * @param path path of Uri
         * @param flags determines what to backup
         * @param isJob backup called from job
         */
        fun makeBackup(context: Context, path: String, flags: Int, isJob: Boolean = false) {
            val intent = Intent(context, BackupCreateService::class.java).apply {
                putExtra(EXTRA_URI, path)
                putExtra(EXTRA_IS_JOB, isJob)
                putExtra(EXTRA_FLAGS, flags)
            }
            context.startService(intent)
        }
    }

    private val backupManager by lazy {
        BackupManager(this).apply {
            setVersion(CURRENT_VERSION)
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) return

        // Get values
        val uri = intent.getStringExtra(EXTRA_URI)
        val isJob = intent.getBooleanExtra(EXTRA_IS_JOB, false)
        val flags = intent.getIntExtra(EXTRA_FLAGS, 0)
        // Create backup
        createBackupFromApp(Uri.parse(uri), flags, isJob)
    }

    /**
     * Create backup Json file from database
     *
     * @param uri path of Uri
     * @param isJob backup called from job
     */
    fun createBackupFromApp(uri: Uri, flags: Int, isJob: Boolean) {
        // Create root object
        val root = JsonObject()

        // Create information object
        val information = JsonObject()

        // Create manga array
        val mangaEntries = JsonArray()

        // Create category array
        val categoryEntries = JsonArray()

        // Add value's to root
        root.add(INFORMATION, information)
        root.add(MANGAS, mangaEntries)
        root.add(CATEGORIES, categoryEntries)

        backupManager.databaseHelper.inTransaction {
            // Get manga from database
            val mangas = backupManager.getFavoriteManga()

            // Set information needed for restore
            information.add(JSON.VERSION, CURRENT_VERSION.toJson())
            information.add(AMOUNT, mangas.size.toJson())

            // Backup library manga and its dependencies
            mangas.forEach { manga ->
                mangaEntries.add(backupManager.backupMangaObject(manga, flags))
            }

            // Backup categories
            if ((flags and BACKUP_CATEGORY_MASK) == BACKUP_CATEGORY) {
                backupManager.backupCategories(categoryEntries)
            }
        }

        try {
            // When BackupCreatorJob
            if (isJob) {
                // Get dir of file
                val dir = UniFile.fromUri(this, uri)

                // Delete oldest backups
                val numberOfBackups = backupManager.numberOfBackups()
                val backupRegex = Regex("""tachiyomi_\d+-\d+-\d+_\d+-\d+.json""")
                dir.listFiles { _, filename -> backupRegex.matches(filename) }
                        .orEmpty()
                        .sortedByDescending { it.name }
                        .drop(numberOfBackups - 1)
                        .forEach { it.delete() }

                // Create new file to place backup
                val date = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
                val newFile = dir.createFile(getString(R.string.backup_file_name, date))
                        ?: throw Exception("Couldn't create backup file")

                newFile.openOutputStream().bufferedWriter().use {
                    backupManager.parser.toJson(root, it)
                }
            } else {
                val file = UniFile.fromUri(this, uri)
                        ?: throw Exception("Couldn't create backup file")
                file.openOutputStream().bufferedWriter().use {
                    backupManager.parser.toJson(root, it)
                }

                // Show completed dialog
                val intent = Intent(SettingsBackupFragment.INTENT_FILTER).apply {
                    putExtra(SettingsBackupFragment.ACTION, SettingsBackupFragment.ACTION_BACKUP_COMPLETED_DIALOG)
                    putExtra(SettingsBackupFragment.EXTRA_URI, file.uri.toString())
                }
                sendLocalBroadcast(intent)
            }
        } catch (e: Exception) {
            Timber.e(e)
            if (!isJob) {
                // Show error dialog
                val intent = Intent(SettingsBackupFragment.INTENT_FILTER).apply {
                    putExtra(SettingsBackupFragment.ACTION, SettingsBackupFragment.ACTION_ERROR_BACKUP_DIALOG)
                    putExtra(SettingsBackupFragment.EXTRA_ERROR_MESSAGE, e.message)
                }
                sendLocalBroadcast(intent)
            }
        }
    }
}