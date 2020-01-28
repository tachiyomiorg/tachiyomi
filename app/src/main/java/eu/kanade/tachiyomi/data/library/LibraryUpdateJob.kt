package eu.kanade.tachiyomi.data.library

import com.evernote.android.job.Job
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class LibraryUpdateJob : Job() {

    override fun onRunJob(params: Params): Result {
        LibraryUpdateService.start(context)
        return Job.Result.SUCCESS
    }

    companion object {
        const val TAG = "LibraryUpdate"

        fun setupTask(prefInterval: Int? = null) {
            val preferences = Injekt.get<PreferencesHelper>()
            val interval = prefInterval ?: preferences.libraryUpdateInterval().getOrDefault()
            if (interval > 0) {
                val restrictions = preferences.libraryUpdateRestriction()!!
                val acRestriction = "ac" in restrictions
                val wifiRestriction = if ("wifi" in restrictions)
                    JobRequest.NetworkType.UNMETERED
                else
                    JobRequest.NetworkType.CONNECTED

                JobRequest.Builder(TAG)
                        .setPeriodic(interval * 60 * 60 * 1000L, 10 * 60 * 1000)
                        .setRequiredNetworkType(wifiRestriction)
                        .setRequiresCharging(acRestriction)
                        .setRequirementsEnforced(true)
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
