package eu.kanade.tachiyomi.data.backup

import android.content.Context
import android.net.Uri
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import java.util.concurrent.TimeUnit
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class BackupCreatorJob(private val context: Context, workerParams: WorkerParameters) :
        Worker(context, workerParams) {

    override fun doWork(): Result {
        val preferences = Injekt.get<PreferencesHelper>()
        val backupManager = BackupManager(context)
        val uri = Uri.parse(preferences.backupsDirectory().getOrDefault())
        val flags = BackupCreateService.BACKUP_ALL
        backupManager.createBackup(uri, flags, true)
        return Result.success()
    }

    companion object {
        private const val TAG = "BackupCreator"

        fun setupTask(context: Context, prefInterval: Int? = null) {
            val preferences = Injekt.get<PreferencesHelper>()
            val interval = prefInterval ?: preferences.backupInterval().get()
            if (interval > 0) {
                val request = PeriodicWorkRequestBuilder<BackupCreatorJob>(
                        interval.toLong(), TimeUnit.HOURS,
                        10, TimeUnit.MINUTES)
                        .addTag(TAG)
                        .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.REPLACE, request)
            } else {
                WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
            }
        }
    }
}
