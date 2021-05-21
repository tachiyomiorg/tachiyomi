package eu.kanade.tachiyomi.ui.security

import android.os.Bundle
import androidx.biometric.BiometricPrompt
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.activity.BaseThemedActivity
import eu.kanade.tachiyomi.util.system.BiometricUtil
import timber.log.Timber
import java.util.Date
import java.util.concurrent.Executors

/**
 * Blank activity with a BiometricPrompt.
 */
class BiometricUnlockActivity : BaseThemedActivity() {

    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Timber.e(errString.toString())
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
