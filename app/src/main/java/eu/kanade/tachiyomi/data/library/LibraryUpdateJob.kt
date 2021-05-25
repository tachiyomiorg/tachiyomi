package eu.kanade.tachiyomi.data.library

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.data.preference.CHARGING
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.UNMETERED_NETWORK
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit

class LibraryUpdateJob(private val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        return if (LibraryUpdateService.start(context)) {
            Result.success()
        } else {
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "LibraryUpdate"

        fun setupTask(context: Context, prefInterval: Int? = null) {
            val preferences = Injekt.get<PreferencesHelper>()
            val interval = prefInterval ?: preferences.libraryUpdateInterval().get()
            if (interval > 0) {
                val restrictions = preferences.libraryUpdateRestriction().get()
                val acRestriction = CHARGING in restrictions
                val wifiRestriction = if (UNMETERED_NETWORK in restrictions) {
                    NetworkType.UNMETERED
                } else {
                    NetworkType.CONNECTED
                }

                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(wifiRestriction)
                    .setRequiresCharging(acRestriction)
                    .build()

                val request = PeriodicWorkRequestBuilder<LibraryUpdateJob>(
                    interval.toLong(),
                    TimeUnit.HOURS,
                    10,
                    TimeUnit.MINUTES
                )
                    .addTag(TAG)
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.REPLACE, request)
            } else {
                WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
            }
        }
    }
}
