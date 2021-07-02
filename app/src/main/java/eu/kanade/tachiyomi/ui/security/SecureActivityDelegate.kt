package eu.kanade.tachiyomi.ui.security

import android.content.Intent
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil
import eu.kanade.tachiyomi.util.view.setSecureScreen
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import uy.kohesive.injekt.injectLazy
import java.util.Date

class SecureActivityDelegate(private val activity: FragmentActivity) {

    private val preferences: PreferencesHelper by injectLazy()

    fun onCreate() {
        preferences.secureScreen().asFlow()
            .onEach { activity.window.setSecureScreen(it || preferences.incognitoMode().get()) }
            .launchIn(activity.lifecycleScope)
    }

    fun onResume() {
        if (preferences.useAuthenticator().get()) {
            if (AuthenticatorUtil.isSupported(activity)) {
                if (isAppLocked()) {
                    activity.startActivity(Intent(activity, UnlockActivity::class.java))
                    activity.overridePendingTransition(0, 0)
                }
            } else {
                preferences.useAuthenticator().set(false)
            }
        }
    }

    private fun isAppLocked(): Boolean {
        if (!locked) {
            return false
        }

        return preferences.lockAppAfter().get() <= 0 ||
            Date().time >= preferences.lastAppUnlock().get() + 60 * 1000 * preferences.lockAppAfter().get()
    }

    companion object {
        var locked: Boolean = true
    }
}
