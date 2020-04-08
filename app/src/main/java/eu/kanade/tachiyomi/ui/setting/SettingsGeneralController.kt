package eu.kanade.tachiyomi.ui.setting

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys
import eu.kanade.tachiyomi.data.preference.PreferenceValues as Values
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.util.preference.defaultValue
import eu.kanade.tachiyomi.util.preference.entriesRes
import eu.kanade.tachiyomi.util.preference.intListPreference
import eu.kanade.tachiyomi.util.preference.listPreference
import eu.kanade.tachiyomi.util.preference.onChange
import eu.kanade.tachiyomi.util.preference.onClick
import eu.kanade.tachiyomi.util.preference.preference
import eu.kanade.tachiyomi.util.preference.preferenceCategory
import eu.kanade.tachiyomi.util.preference.titleRes
import eu.kanade.tachiyomi.util.system.LocaleHelper

class SettingsGeneralController : SettingsController() {

    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.pref_category_general

        intListPreference {
            key = Keys.startScreen
            titleRes = R.string.pref_start_screen
            entriesRes = arrayOf(
                    R.string.label_library,
                    R.string.label_recent_updates,
                    R.string.label_recent_manga)
            entryValues = arrayOf("1", "3", "2")
            defaultValue = "1"
            summary = "%s"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            preference {
                titleRes = R.string.pref_manage_notifications
                onClick {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    startActivity(intent)
                }
            }
        }

        preferenceCategory {
            titleRes = R.string.pref_category_display

            listPreference {
                key = Keys.lang
                titleRes = R.string.pref_language

                val langs = mutableListOf<Pair<String, String>>()
                langs += Pair("", context.getString(R.string.system_default))
                langs += arrayOf(
                    "ar", "bg", "bn", "ca", "cs", "de", "el", "en-US", "en-GB", "es", "fr", "he",
                    "hi", "hu", "in", "it", "ja", "ko", "lv", "ms", "nb-rNO", "nl", "pl", "pt",
                    "pt-BR", "ro", "ru", "sc", "sr", "sv", "th", "tl", "tr", "uk", "vi", "zh-rCN"
                )
                    .map {
                        val locale = LocaleHelper.getLocaleFromString(it)
                        Pair(it, locale!!.getDisplayName(locale).capitalize())
                    }
                    .sortedBy { it.second }

                entryValues = langs.map { it.first }.toTypedArray()
                entries = langs.map { it.second }.toTypedArray()
                defaultValue = ""
                summary = "%s"

                onChange { newValue ->
                    val activity = activity ?: return@onChange false
                    val app = activity.application
                    LocaleHelper.changeLocale(newValue.toString())
                    LocaleHelper.updateConfiguration(app, app.resources.configuration)
                    activity.recreate()
                    true
                }
            }
            listPreference {
                key = Keys.dateFormat
                titleRes = R.string.pref_date_format
                entryValues = arrayOf("", "MM/dd/yy", "dd/MM/yy", "yyyy-MM-dd")
                entries = entryValues.map { value ->
                    if (value == "") {
                        context.getString(R.string.system_default)
                    } else {
                        value
                    }
                }.toTypedArray()
                defaultValue = ""
                summary = "%s"
            }
            listPreference {
                key = Keys.themeMode
                titleRes = R.string.pref_theme_mode

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    entriesRes = arrayOf(
                            R.string.theme_system,
                            R.string.theme_light,
                            R.string.theme_dark)
                    entryValues = arrayOf(
                            Values.THEME_MODE_SYSTEM,
                            Values.THEME_MODE_LIGHT,
                            Values.THEME_MODE_DARK)
                    defaultValue = Values.THEME_MODE_SYSTEM
                } else {
                    entriesRes = arrayOf(
                            R.string.theme_light,
                            R.string.theme_dark)
                    entryValues = arrayOf(
                            Values.THEME_MODE_LIGHT,
                            Values.THEME_MODE_DARK)
                    defaultValue = Values.THEME_MODE_LIGHT
                }

                summary = "%s"

                onChange {
                    activity?.recreate()
                    true
                }
            }
            listPreference {
                key = Keys.themeLight
                titleRes = R.string.pref_theme_light
                entriesRes = arrayOf(
                        R.string.theme_light_default,
                        R.string.theme_light_blue)
                entryValues = arrayOf(
                        Values.THEME_LIGHT_DEFAULT,
                        Values.THEME_LIGHT_BLUE)
                defaultValue = Values.THEME_LIGHT_DEFAULT
                summary = "%s"

                preferences.themeMode().asObservable()
                        .subscribeUntilDestroy { isVisible = it != Values.THEME_MODE_DARK }

                onChange {
                    if (preferences.themeMode().getOrDefault() != Values.THEME_MODE_DARK) {
                        activity?.recreate()
                    }
                    true
                }
            }
            listPreference {
                key = Keys.themeDark
                titleRes = R.string.pref_theme_dark
                entriesRes = arrayOf(
                        R.string.theme_dark_default,
                        R.string.theme_dark_blue,
                        R.string.theme_dark_amoled)
                entryValues = arrayOf(
                        Values.THEME_DARK_DEFAULT,
                        Values.THEME_DARK_BLUE,
                        Values.THEME_DARK_AMOLED)
                defaultValue = Values.THEME_DARK_DEFAULT
                summary = "%s"

                preferences.themeMode().asObservable()
                        .subscribeUntilDestroy { isVisible = it != Values.THEME_MODE_LIGHT }

                onChange {
                    if (preferences.themeMode().getOrDefault() != Values.THEME_MODE_LIGHT) {
                        activity?.recreate()
                    }
                    true
                }
            }
        }
    }
}
