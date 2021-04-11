package eu.kanade.tachiyomi.ui.security

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.system.BiometricUtil
import uy.kohesive.injekt.injectLazy
import java.util.Date
import java.util.concurrent.Executors

/**
 * Blank activity with a BiometricPrompt.
 */
class BiometricUnlockActivity : AppCompatActivity() {

    private val preferences: PreferencesHelper by injectLazy()
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    finishAffinity()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    SecureActivityDelegate.locked = false
                    preferences.lastAppUnlock().set(Date().time)
                    finish()
                }
            }
        )

        var promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.unlock_app))
            .setAllowedAuthenticators(BiometricUtil.getSupportedAuthenticators(this))
            .setConfirmationRequired(false)

        if (!BiometricUtil.isDeviceCredentialAllowed(this)) {
            promptInfo = promptInfo.setNegativeButtonText(getString(R.string.action_cancel))
        }

        biometricPrompt.authenticate(promptInfo.build())
    }
}
