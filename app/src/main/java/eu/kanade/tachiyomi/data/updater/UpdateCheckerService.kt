package eu.kanade.tachiyomi.data.updater

import android.content.Context
import android.support.v4.app.NotificationCompat
import com.google.android.gms.gcm.*
import eu.kanade.tachiyomi.Constants.NOTIFICATION_UPDATER_ID
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.notificationManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class UpdateCheckerService : GcmTaskService() {

    override fun onInitializeTasks() {
        val preferences: PreferencesHelper = Injekt.get()
        if (preferences.automaticUpdates()) {
            setupTask(this)
        }
    }

    override fun onRunTask(params: TaskParams): Int {
        return checkVersion()
    }

    fun checkVersion(): Int {
        return GithubUpdateChecker()
                .checkForUpdate()
                .map { result ->
                    if (result is GithubUpdateResult.NewUpdate) {
                        val url = result.release.downloadLink

                        NotificationCompat.Builder(this).update {
                            setContentTitle(getString(R.string.app_name))
                            setContentText(getString(R.string.update_check_notification_update_available))
                            setSmallIcon(android.R.drawable.stat_sys_download_done)
                            // Download action
                            addAction(android.R.drawable.stat_sys_download_done,
                                    getString(R.string.action_download),
                                    UpdateNotificationReceiver.downloadApkIntent(
                                            this@UpdateCheckerService, url))
                        }
                    }
                    GcmNetworkManager.RESULT_SUCCESS
                }
                .onErrorReturn { GcmNetworkManager.RESULT_FAILURE }
                // Sadly, the task needs to be synchronous.
                .toBlocking()
                .single()
    }

    fun NotificationCompat.Builder.update(block: NotificationCompat.Builder.() -> Unit) {
        block()
        notificationManager.notify(NOTIFICATION_UPDATER_ID, build())
    }

    companion object {
        fun setupTask(context: Context) {
            val task = PeriodicTask.Builder()
                    .setService(UpdateCheckerService::class.java)
                    .setTag("Updater")
                    // 24 hours
                    .setPeriod(24 * 60 * 60)
                    // Run between the last two hours
                    .setFlex(2 * 60 * 60)
                    .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                    .setPersisted(true)
                    .setUpdateCurrent(true)
                    .build()

            GcmNetworkManager.getInstance(context).schedule(task)
        }

        fun cancelTask(context: Context) {
            GcmNetworkManager.getInstance(context).cancelAllTasks(UpdateCheckerService::class.java)
        }

    }

}