package eu.kanade.tachiyomi.ui.security

import android.content.Intent
import android.view.WindowManager
import androidx.biometric.BiometricManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import uy.kohesive.injekt.injectLazy
import java.util.Date

class SecureActivityDelegate(private val activity: FragmentActivity) {

    private val preferences by injectLazy<PreferencesHelper>()

    fun onCreate() {
        preferences.secureScreen().asFlow()
            .onEach {
                if (it) {
                    activity.window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
            .launchIn(activity.lifecycleScope)
    }

    fun onResume() {
        val lockApp = preferences.useBiometricLock().get()
        if (lockApp && BiometricManager.from(activity).canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS) {
            if (isAppLocked()) {
                val intent = Intent(activity, BiometricUnlockActivity::class.java)
                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
            }
        } else if (lockApp) {
            preferences.useBiometricLock().set(false)
        }
    }

    private fun isAppLocked(): Boolean {
        return locked &&
            (
                preferences.lockAppAfter().get() <= 0 ||
                    Date().time >= preferences.lastAppUnlock().get() + 60 * 1000 * preferences.lockAppAfter().get()
                )
    }

    companion object {
        var locked: Boolean = true
    }
}
