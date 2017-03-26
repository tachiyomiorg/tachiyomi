package eu.kanade.tachiyomi.data.backup

import com.evernote.android.job.Job
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class BackupCreatorJob : Job() {

    override fun onRunJob(params: Params): Result {
        val preferences = Injekt.get<PreferencesHelper>()
        val path = preferences.backupsDirectory().getOrDefault()
        val flags = BackupCreateService.BACKUP_ALL
        BackupCreateService.makeBackup(context,path,flags,true)
        return Result.SUCCESS
    }

    companion object {
        const val TAG = "BackupCreator"

        fun setupTask(prefInterval: Int? = null) {
            val preferences = Injekt.get<PreferencesHelper>()
            val interval = prefInterval ?: preferences.backupInterval().getOrDefault()
            if (interval > 0) {
                JobRequest.Builder(TAG)
                        .setPeriodic(interval * 60 * 60 * 1000L, 10 * 60 * 1000)
                        .setPersisted(true)
                        .setUpdateCurrent(true)
                        .build()
                        .schedule()
            }
        }

        fun cancelTask() {
            JobManager.instance().cancelAllForTag(TAG)
        }
    }
}