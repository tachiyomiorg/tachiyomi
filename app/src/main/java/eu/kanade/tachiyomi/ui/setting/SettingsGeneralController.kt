package eu.kanade.tachiyomi.ui.setting

import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.preference.*
import eu.kanade.tachiyomi.util.system.LocaleHelper
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys

class SettingsGeneralController : SettingsController() {

    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.pref_category_general

        listPreference {
            key = Keys.lang
            titleRes = R.string.pref_language
            entryValues = arrayOf("", "ar", "bg", "bn", "ca", "cs", "de", "el", "en-US", "en-GB",
                    "es", "fr", "hi", "hu", "in", "it", "ja", "ko", "lv", "ms", "nb-rNO", "nl", "pl", "pt",
                    "pt-BR", "ro", "ru", "sc", "sr", "sv", "th", "tl", "tr", "uk", "vi", "zh-rCN")
            entries = entryValues.map { value ->
                val locale = LocaleHelper.getLocaleFromString(value.toString())
                locale?.getDisplayName(locale)?.capitalize()
                        ?: context.getString(R.string.system_default)
            }.toTypedArray()
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
        intListPreference {
            key = Keys.themeMode
            titleRes = R.string.pref_theme_mode
            entriesRes = arrayOf(
                    R.string.theme_light,
                    R.string.theme_dark,
                    R.string.theme_system)
            entryValues = arrayOf("1", "2", "3")
            defaultValue = "1"
            summary = "%s"

            onChange {
                activity?.recreate()
                true
            }
        }
        intListPreference {
            key = Keys.themeDark
            titleRes = R.string.pref_theme_dark
            entriesRes = arrayOf(
                    R.string.theme_dark_default,
                    R.string.theme_dark_amoled,
                    R.string.theme_dark_blue)
            entryValues = arrayOf("1", "2", "3")
            defaultValue = "1"
            summary = "%s"

            onChange {
                if (preferences.themeMode() != 1) {
                    activity?.recreate()
                }
                true
            }
        }
        intListPreference {
            key = Keys.startScreen
            titleRes = R.string.pref_start_screen
            entriesRes = arrayOf(R.string.label_library, R.string.label_recent_manga,
                    R.string.label_recent_updates)
            entryValues = arrayOf("1", "2", "3")
            defaultValue = "1"
            summary = "%s"
        }
    }

}
