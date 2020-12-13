package eu.kanade.tachiyomi.data.backup

import android.content.Context
import androidx.core.net.toUri
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.data.backup.full.FullBackupManager
import eu.kanade.tachiyomi.data.backup.legacy.LegacyBackupManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit

class BackupCreatorJob(private val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        val preferences = Injekt.get<PreferencesHelper>()
        val uri = preferences.backupsDirectory().get().toUri()
        val flags = BackupCreateService.BACKUP_ALL
        return try {
            FullBackupManager(context).createBackup(uri, flags, true)
            if (preferences.createLegacyBackup().get()) {
                LegacyBackupManager(context).createBackup(uri, flags, true)
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "BackupCreator"

        fun setupTask(context: Context, prefInterval: Int? = null) {
            val preferences = Injekt.get<PreferencesHelper>()
            val interval = prefInterval ?: preferences.backupInterval().get()
            if (interval > 0) {
                val request = PeriodicWorkRequestBuilder<BackupCreatorJob>(
                    interval.toLong(),
                    TimeUnit.HOURS,
                    10,
                    TimeUnit.MINUTES
                )
                    .addTag(TAG)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.REPLACE, request)
            } else {
                WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
            }
        }
    }
}
