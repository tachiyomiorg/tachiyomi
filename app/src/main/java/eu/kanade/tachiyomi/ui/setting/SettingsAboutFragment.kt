package eu.kanade.tachiyomi.ui.setting

import android.os.Bundle
import android.support.v7.preference.XpPreferenceFragment
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.updater.GithubUpdateChecker
import eu.kanade.tachiyomi.data.updater.GithubUpdateResult
import eu.kanade.tachiyomi.data.updater.UpdateCheckerService
import eu.kanade.tachiyomi.data.updater.UpdateDownloaderService
import eu.kanade.tachiyomi.util.toast
import net.xpece.android.support.preference.SwitchPreference
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import timber.log.Timber
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class SettingsAboutFragment : SettingsFragment() {
    /**
     * Checks for new releases
     */
    private val updateChecker by lazy { GithubUpdateChecker() }

    /**
     * The subscribtion service of the obtained release object
     */
    private var releaseSubscription: Subscription? = null

    val automaticUpdates by lazy {
        findPreference(getString(R.string.pref_enable_automatic_updates_key)) as SwitchPreference
    }

    companion object {
        fun newInstance(rootKey: String): SettingsAboutFragment {
            val args = Bundle()
            args.putString(XpPreferenceFragment.ARG_PREFERENCE_ROOT, rootKey)
            return SettingsAboutFragment().apply { arguments = args }
        }
    }

    override fun onViewCreated(view: View, savedState: Bundle?) {
        super.onViewCreated(view, savedState)

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

            automaticUpdates.setOnPreferenceChangeListener { preference, any ->
                val checked = any as Boolean
                if (checked) {
                    UpdateCheckerService.setupTask(context)
                } else {
                    UpdateCheckerService.cancelTask(context)
                }
                true
            }
        } else {
            automaticUpdates.isVisible = false
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

        context.toast(R.string.update_check_look_for_updates)

        releaseSubscription = updateChecker.checkForUpdate()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ result ->
                    when (result) {
                        is GithubUpdateResult.NewUpdate -> {
                            val body = result.release.changeLog
                            val url = result.release.downloadLink

                            // Create confirmation window
                            MaterialDialog.Builder(context)
                                    .title(R.string.update_check_title)
                                    .content(body)
                                    .positiveText(getString(R.string.update_check_confirm))
                                    .negativeText(getString(R.string.update_check_ignore))
                                    .onPositive { dialog, which ->
                                        // Start download
                                        UpdateDownloaderService.downloadUpdate(context, url)
                                    }
                                    .show()
                        }
                        is GithubUpdateResult.NoNewUpdate -> {
                            context.toast(R.string.update_check_no_new_updates)
                        }
                    }
                }, { error ->
                    Timber.e(error, error.message)
                })
    }

}
