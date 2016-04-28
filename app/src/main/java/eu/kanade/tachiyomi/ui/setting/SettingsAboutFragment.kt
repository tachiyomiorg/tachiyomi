package eu.kanade.tachiyomi.ui.setting

import android.os.Bundle
import android.support.v7.preference.SwitchPreferenceCompat
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.updater.GithubUpdateChecker
import eu.kanade.tachiyomi.data.updater.UpdateDownloader
import eu.kanade.tachiyomi.util.toast
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class SettingsAboutFragment : SettingsNestedFragment() {
    /**
     * Checks for new releases
     */
    private val updateChecker by lazy { GithubUpdateChecker(activity) }

    /**
     * The subscribtion service of the obtained release object
     */
    private var releaseSubscription: Subscription? = null

    val automaticUpdateToggle by lazy {
        findPreference(getString(R.string.pref_enable_automatic_updates_key)) as SwitchPreferenceCompat
    }

    companion object {

        fun newInstance(resourcePreference: Int, resourceTitle: Int): SettingsNestedFragment {
            val fragment = SettingsAboutFragment()
            fragment.setArgs(resourcePreference, resourceTitle)
            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val version = findPreference(getString(R.string.pref_version))
        val buildTime = findPreference(getString(R.string.pref_build_time))

        version.summary = if (BuildConfig.DEBUG)
            "r" + BuildConfig.COMMIT_COUNT
        else
            BuildConfig.VERSION_NAME

        if (!BuildConfig.DEBUG && BuildConfig.INCLUDE_UPDATER) {
            //Set onClickListener to check for new version
            version.setOnPreferenceClickListener {
                checkVersion()
                true
            }

            //TODO One glorious day enable this and add the magnificent option for auto update checking.
            // automaticUpdateToggle.isEnabled = true
            //            automaticUpdateToggle.setOnPreferenceChangeListener { preference, any ->
            //                val status = any as Boolean
            //                UpdateDownloaderAlarm.startAlarm(activity, 12, status)
            //                true
            //            }
        }

        buildTime.summary = getFormattedBuildTime()
    }

    override fun onDestroyView() {
        releaseSubscription?.unsubscribe()
        super.onDestroyView()
    }

    private fun getFormattedBuildTime(): String {
        try {
            val inputDf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'")
            inputDf.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputDf.parse(BuildConfig.BUILD_TIME)

            val outputDf = DateFormat.getDateTimeInstance(
                    DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
            outputDf.timeZone = TimeZone.getDefault()

            return outputDf.format(date)
        } catch (e: ParseException) {
            return BuildConfig.BUILD_TIME
        }
    }

    /**
     * Checks version and shows a user prompt if an update is available.
     */
    private fun checkVersion() {
        releaseSubscription?.unsubscribe()

        releaseSubscription = updateChecker.checkForApplicationUpdate()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ release ->
                    //Get version of latest release
                    var newVersion = release.version
                    newVersion = newVersion.replace("[^\\d.]".toRegex(), "")

                    //Check if latest version is different from current version
                    if (newVersion != BuildConfig.VERSION_NAME) {
                        val downloadLink = release.downloadLink
                        val body = release.changeLog

                        //Create confirmation window
                        MaterialDialog.Builder(activity)
                                .title(R.string.update_check_title)
                                .content(body)
                                .positiveText(getString(R.string.update_check_confirm))
                                .negativeText(getString(R.string.update_check_ignore))
                                .onPositive { dialog, which ->
                                    // User output that download has started
                                    activity.toast(R.string.update_check_download_started)
                                    // Start download
                                    UpdateDownloader(activity.applicationContext).execute(downloadLink)
                                }.show()
                    } else {
                        activity.toast(R.string.update_check_no_new_updates)
                    }
                }, {
                    it.printStackTrace()
                })
    }

}
