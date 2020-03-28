package eu.kanade.tachiyomi.ui.security

import android.content.Intent
import android.view.WindowManager
import androidx.biometric.BiometricManager
import androidx.fragment.app.FragmentActivity
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import java.util.Date
import rx.Subscription
import uy.kohesive.injekt.injectLazy

class SecureActivityDelegate(private val activity: FragmentActivity) {

    private val preferences by injectLazy<PreferencesHelper>()

    private var secureScreenSubscription: Subscription? = null

    fun onCreate() {
        secureScreenSubscription = preferences.secureScreen().asObservable()
                .subscribe {
                    if (it) {
                        activity.window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
    }

    fun onResume() {
        val lockApp = preferences.useBiometricLock().getOrDefault()
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

    fun onDestroy() {
        secureScreenSubscription?.unsubscribe()
    }

    private fun isAppLocked(): Boolean {
        return locked &&
                (preferences.lockAppAfter().getOrDefault() <= 0 ||
                        Date().time >= preferences.lastAppUnlock().getOrDefault() + 60 * 1000 * preferences.lockAppAfter().getOrDefault())
    }

    companion object {
        var locked: Boolean = true
    }
}
