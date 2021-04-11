package eu.kanade.tachiyomi.ui.setting

import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.asImmediateFlow
import eu.kanade.tachiyomi.util.preference.defaultValue
import eu.kanade.tachiyomi.util.preference.intListPreference
import eu.kanade.tachiyomi.util.preference.summaryRes
import eu.kanade.tachiyomi.util.preference.switchPreference
import eu.kanade.tachiyomi.util.preference.titleRes
import eu.kanade.tachiyomi.util.system.BiometricUtil
import kotlinx.coroutines.flow.launchIn
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys

class SettingsSecurityController : SettingsController() {

    override fun setupPreferenceScreen(screen: PreferenceScreen) = screen.apply {
        titleRes = R.string.pref_category_security

        if (BiometricUtil.isSupported(context)) {
            switchPreference {
                key = Keys.useBiometricLock
                titleRes = R.string.lock_with_biometrics
                defaultValue = false
            }
            intListPreference {
                key = Keys.lockAppAfter
                titleRes = R.string.lock_when_idle
                val values = arrayOf("0", "1", "2", "5", "10", "-1")
                entries = values.mapNotNull {
                    when (it) {
                        "-1" -> context.getString(R.string.lock_never)
                        "0" -> context.getString(R.string.lock_always)
                        else -> resources?.getQuantityString(R.plurals.lock_after_mins, it.toInt(), it)
                    }
                }.toTypedArray()
                entryValues = values
                defaultValue = "0"
                summary = "%s"

                preferences.useBiometricLock().asImmediateFlow { isVisible = it }
                    .launchIn(viewScope)
            }
        }

        switchPreference {
            key = Keys.secureScreen
            titleRes = R.string.secure_screen
            summaryRes = R.string.secure_screen_summary
            defaultValue = false
        }
        switchPreference {
            key = Keys.hideNotificationContent
            titleRes = R.string.hide_notification_content
            defaultValue = false
        }
    }
}
