package eu.kanade.tachiyomi.ui.more

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.preference.PreferenceScreen
import com.afollestad.materialdialogs.MaterialDialog
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.data.updater.UpdateChecker
import eu.kanade.tachiyomi.data.updater.UpdateResult
import eu.kanade.tachiyomi.data.updater.UpdaterJob
import eu.kanade.tachiyomi.data.updater.UpdaterService
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.main.ChangelogDialogController
import eu.kanade.tachiyomi.ui.setting.SettingsController
import eu.kanade.tachiyomi.util.lang.launchNow
import eu.kanade.tachiyomi.util.lang.toDateTimestampString
import eu.kanade.tachiyomi.util.preference.defaultValue
import eu.kanade.tachiyomi.util.preference.onChange
import eu.kanade.tachiyomi.util.preference.onClick
import eu.kanade.tachiyomi.util.preference.preference
import eu.kanade.tachiyomi.util.preference.summaryRes
import eu.kanade.tachiyomi.util.preference.switchPreference
import eu.kanade.tachiyomi.util.preference.titleRes
import eu.kanade.tachiyomi.util.system.toast
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import timber.log.Timber
import uy.kohesive.injekt.injectLazy

class AboutController : SettingsController() {

    /**
     * Checks for new releases
     */
    private val updateChecker by lazy { UpdateChecker.getUpdateChecker() }

    private val userPreferences: PreferencesHelper by injectLazy()

    private val dateFormat: DateFormat = userPreferences.dateFormat().getOrDefault()

    private val isUpdaterEnabled = BuildConfig.INCLUDE_UPDATER

    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.pref_category_about

        switchPreference {
            key = Keys.automaticUpdates
            titleRes = R.string.pref_enable_automatic_updates
            summaryRes = R.string.pref_enable_automatic_updates_summary
            defaultValue = true

            if (isUpdaterEnabled) {
                onChange { newValue ->
                    val checked = newValue as Boolean
                    if (checked) {
                        UpdaterJob.setupTask(context)
                    } else {
                        UpdaterJob.cancelTask(context)
                    }
                    true
                }
            } else {
                isVisible = false
            }
        }
        preference {
            title = "Discord"
            val url = "https://discord.gg/tachiyomi"
            summary = url
            onClick {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
        }
        preference {
            title = "GitHub"
            val url = "https://github.com/inorichi/tachiyomi"
            summary = url
            onClick {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
        }
        preference {
            titleRes = R.string.version
            summary = if (BuildConfig.DEBUG)
                "r${BuildConfig.COMMIT_COUNT} (${BuildConfig.COMMIT_SHA})"
            else
                BuildConfig.VERSION_NAME

            if (isUpdaterEnabled) {
                onClick { checkVersion() }
            }
        }
        preference {
            titleRes = R.string.build_time
            summary = getFormattedBuildTime()

            onClick {
                ChangelogDialogController().showDialog(router)
            }
        }
    }

    /**
     * Checks version and shows a user prompt if an update is available.
     */
    private fun checkVersion() {
        if (activity == null) return

        activity?.toast(R.string.update_check_look_for_updates)

        launchNow {
            try {
                when (val result = updateChecker.checkForUpdate()) {
                    is UpdateResult.NewUpdate<*> -> {
                        val body = result.release.info
                        val url = result.release.downloadLink

                        // Create confirmation window
                        NewUpdateDialogController(body, url).showDialog(router)
                    }
                    is UpdateResult.NoNewUpdate -> {
                        activity?.toast(R.string.update_check_no_new_updates)
                    }
                }
            } catch (error: Exception) {
                activity?.toast(error.message)
                Timber.e(error)
            }
        }
    }

    class NewUpdateDialogController(bundle: Bundle? = null) : DialogController(bundle) {

        constructor(body: String, url: String) : this(Bundle().apply {
            putString(BODY_KEY, body)
            putString(URL_KEY, url)
        })

        override fun onCreateDialog(savedViewState: Bundle?): Dialog {
            return MaterialDialog.Builder(activity!!)
                    .title(R.string.update_check_notification_update_available)
                    .content(args.getString(BODY_KEY) ?: "")
                    .positiveText(R.string.update_check_confirm)
                    .negativeText(R.string.update_check_ignore)
                    .onPositive { _, _ ->
                        val appContext = applicationContext
                        if (appContext != null) {
                            // Start download
                            val url = args.getString(URL_KEY) ?: ""
                            UpdaterService.downloadUpdate(appContext, url)
                        }
                    }
                    .build()
        }

        private companion object {
            const val BODY_KEY = "NewUpdateDialogController.body"
            const val URL_KEY = "NewUpdateDialogController.key"
        }
    }

    private fun getFormattedBuildTime(): String {
        try {
            val inputDf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.US)
            inputDf.timeZone = TimeZone.getTimeZone("UTC")
            val buildTime = inputDf.parse(BuildConfig.BUILD_TIME)

            val outputDf = DateFormat.getDateTimeInstance(
                    DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
            outputDf.timeZone = TimeZone.getDefault()

            return buildTime.toDateTimestampString(dateFormat)
        } catch (e: ParseException) {
            return BuildConfig.BUILD_TIME
        }
    }
}
