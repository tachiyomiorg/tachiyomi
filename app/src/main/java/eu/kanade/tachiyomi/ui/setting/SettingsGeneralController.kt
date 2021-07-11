package eu.kanade.tachiyomi.ui.setting

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.asImmediateFlow
import eu.kanade.tachiyomi.util.preference.defaultValue
import eu.kanade.tachiyomi.util.preference.entriesRes
import eu.kanade.tachiyomi.util.preference.intListPreference
import eu.kanade.tachiyomi.util.preference.listPreference
import eu.kanade.tachiyomi.util.preference.onChange
import eu.kanade.tachiyomi.util.preference.onClick
import eu.kanade.tachiyomi.util.preference.preference
import eu.kanade.tachiyomi.util.preference.preferenceCategory
import eu.kanade.tachiyomi.util.preference.switchPreference
import eu.kanade.tachiyomi.util.preference.titleRes
import eu.kanade.tachiyomi.util.system.LocaleHelper
import eu.kanade.tachiyomi.util.system.isTablet
import kotlinx.coroutines.flow.launchIn
import java.util.Date
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys
import eu.kanade.tachiyomi.data.preference.PreferenceValues as Values

class SettingsGeneralController : SettingsController() {

    override fun setupPreferenceScreen(screen: PreferenceScreen) = screen.apply {
        titleRes = R.string.pref_category_general

        intListPreference {
            key = Keys.startScreen
            titleRes = R.string.pref_start_screen
            entriesRes = arrayOf(
                R.string.label_library,
                R.string.label_recent_updates,
                R.string.label_recent_manga,
                R.string.browse
            )
            entryValues = arrayOf("1", "3", "2", "4")
            defaultValue = "1"
            summary = "%s"
        }
        switchPreference {
            key = Keys.confirmExit
            titleRes = R.string.pref_confirm_exit
            defaultValue = false
        }
        if (context.isTablet()) {
            intListPreference {
                key = Keys.sideNavIconAlignment
                titleRes = R.string.pref_side_nav_icon_alignment
                entriesRes = arrayOf(
                    R.string.alignment_top,
                    R.string.alignment_center,
                    R.string.alignment_bottom,
                )
                entryValues = arrayOf("0", "1", "2")
                defaultValue = "0"
                summary = "%s"
            }
        } else {
            switchPreference {
                key = Keys.hideBottomBarOnScroll
                titleRes = R.string.pref_hide_bottom_bar_on_scroll
                defaultValue = true
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            preference {
                key = "pref_manage_notifications"
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
            titleRes = R.string.pref_category_theme

            listPreference {
                key = Keys.themeMode
                titleRes = R.string.pref_theme_mode

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    entriesRes = arrayOf(
                        R.string.theme_system,
                        R.string.theme_light,
                        R.string.theme_dark
                    )
                    entryValues = arrayOf(
                        Values.ThemeMode.system.name,
                        Values.ThemeMode.light.name,
                        Values.ThemeMode.dark.name
                    )
                    defaultValue = Values.ThemeMode.system.name
                } else {
                    entriesRes = arrayOf(
                        R.string.theme_light,
                        R.string.theme_dark
                    )
                    entryValues = arrayOf(
                        Values.ThemeMode.light.name,
                        Values.ThemeMode.dark.name
                    )
                    defaultValue = Values.ThemeMode.light.name
                }

                summary = "%s"

                onChange {
                    activity?.recreate()
                    true
                }
            }
            listPreference {
                key = Keys.appTheme
                titleRes = R.string.pref_app_theme

                val appThemes = Values.AppTheme.values().filter { it.titleResId != null }
                entriesRes = appThemes.map { it.titleResId!! }.toTypedArray()
                entryValues = appThemes.map { it.name }.toTypedArray()
                defaultValue = appThemes[0].name
                summary = "%s"

                onChange {
                    activity?.recreate()
                    true
                }
            }
            switchPreference {
                key = Keys.themeDarkAmoled
                titleRes = R.string.pref_dark_theme_pure_black
                defaultValue = false

                preferences.themeMode().asImmediateFlow { isVisible = it != Values.ThemeMode.light }
                    .launchIn(viewScope)

                onChange {
                    activity?.recreate()
                    true
                }
            }
        }

        preferenceCategory {
            titleRes = R.string.pref_category_locale

            listPreference {
                key = Keys.lang
                titleRes = R.string.pref_language

                val langs = mutableListOf<Pair<String, String>>()
                langs += Pair(
                    "",
                    "${context.getString(R.string.system_default)} (${LocaleHelper.getDisplayName("")})"
                )
                // Due to compatibility issues:
                // - Hebrew: `he` is copied into `iw` at build time
                langs += arrayOf(
                    "am",
                    "ar",
                    "be",
                    "bg",
                    "bn",
                    "ca",
                    "cs",
                    "cv",
                    "de",
                    "el",
                    "eo",
                    "es",
                    "es-419",
                    "en",
                    "fa",
                    "fi",
                    "fil",
                    "fr",
                    "gl",
                    "he",
                    "hi",
                    "hr",
                    "hu",
                    "in",
                    "it",
                    "ja",
                    "jv",
                    "ka-rGE",
                    "kn",
                    "ko",
                    "lt",
                    "lv",
                    "mr",
                    "ms",
                    "my",
                    "nb-rNO",
                    "ne",
                    "nl",
                    "pl",
                    "pt",
                    "pt-BR",
                    "ro",
                    "ru",
                    "sah",
                    "sc",
                    "sk",
                    "sr",
                    "sv",
                    "te",
                    "th",
                    "tr",
                    "uk",
                    "ur-rPK",
                    "vi",
                    "uz",
                    "zh-rCN",
                    "zh-rTW"
                )
                    .map {
                        Pair(it, LocaleHelper.getDisplayName(it))
                    }
                    .sortedBy { it.second }

                entryValues = langs.map { it.first }.toTypedArray()
                entries = langs.map { it.second }.toTypedArray()
                defaultValue = ""
                summary = "%s"

                onChange { newValue ->
                    activity?.recreate()
                    true
                }
            }
            listPreference {
                key = Keys.dateFormat
                titleRes = R.string.pref_date_format
                entryValues = arrayOf("", "MM/dd/yy", "dd/MM/yy", "yyyy-MM-dd", "dd MMM yyyy", "MMM dd, yyyy")

                val now = Date().time
                entries = entryValues.map { value ->
                    val formattedDate = preferences.dateFormat(value.toString()).format(now)
                    if (value == "") {
                        "${context.getString(R.string.system_default)} ($formattedDate)"
                    } else {
                        "$value ($formattedDate)"
                    }
                }.toTypedArray()

                defaultValue = ""
                summary = "%s"
            }
        }
    }
}
