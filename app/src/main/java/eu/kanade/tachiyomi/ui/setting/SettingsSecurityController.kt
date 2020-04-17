package eu.kanade.tachiyomi.ui.setting

import androidx.biometric.BiometricManager
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys
import eu.kanade.tachiyomi.util.preference.defaultValue
import eu.kanade.tachiyomi.util.preference.intListPreference
import eu.kanade.tachiyomi.util.preference.summaryRes
import eu.kanade.tachiyomi.util.preference.switchPreference
import eu.kanade.tachiyomi.util.preference.titleRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SettingsSecurityController : SettingsController() {

    private val uiScope = CoroutineScope(Dispatchers.Main)

    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.pref_category_security

        if (BiometricManager.from(context).canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS) {
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

                isVisible = preferences.useBiometricLock().get()
                preferences.useBiometricLock().asFlow()
                    .onEach { isVisible = it }
                    .launchIn(uiScope)
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
