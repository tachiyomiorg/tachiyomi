package eu.kanade.tachiyomi.extension

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.extension.api.ExtensionGithubApi
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.util.system.notification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit

class ExtensionUpdateJob(private val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        GlobalScope.launch(Dispatchers.IO) {
            val pendingUpdates = try {
                ExtensionGithubApi().checkForUpdates(context)
            } catch (e: Exception) {
                emptyList<Extension.Installed>()
            }
            if (pendingUpdates.isNotEmpty()) {
                val names = pendingUpdates.map { it.name }
                NotificationManagerCompat.from(context).apply {
                    notify(Notifications.ID_UPDATES_TO_EXTS,
                        context.notification(Notifications.CHANNEL_UPDATES_TO_EXTS) {
                            setContentTitle(
                                context.resources.getQuantityString(
                                    R.plurals.update_check_notification_ext_updates, names
                                        .size, names.size
                                )
                            )
                            val extNames = if (names.size > 5) {
                                "${names.take(4).joinToString(", ")}, " +
                                    context.resources.getQuantityString(
                                        R.plurals.notification_and_n_more_ext,
                                        (names.size - 4), (names.size - 4)
                                    )
                            } else names.joinToString(", ")
                            setContentText(extNames)
                            setStyle(NotificationCompat.BigTextStyle().bigText(extNames))
                            setSmallIcon(R.drawable.ic_extension_update)
                            setContentIntent(
                                NotificationReceiver.openExtensionsPendingActivity(
                                    context
                                )
                            )
                            setAutoCancel(true)
                        })
                }
            }
        }
        return Result.success()
    }

    companion object {
        const val TAG = "ExtensionUpdate"

        fun setupTask(context: Context, forceAutoUpdateJob: Boolean? = null) {
            val preferences = Injekt.get<PreferencesHelper>()
            val autoUpdateJob = forceAutoUpdateJob ?: preferences.automaticExtUpdates().getOrDefault()
            if (autoUpdateJob) {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val request = PeriodicWorkRequestBuilder<ExtensionUpdateJob>(
                    12, TimeUnit.HOURS,
                    1, TimeUnit.HOURS)
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
