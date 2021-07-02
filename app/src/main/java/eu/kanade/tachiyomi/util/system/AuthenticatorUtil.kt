package eu.kanade.tachiyomi.util.system

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators

object AuthenticatorUtil {

    fun getSupportedAuthenticators(context: Context): Int {
        if (isLegacySecured(context)) {
            return Authenticators.BIOMETRIC_WEAK or Authenticators.DEVICE_CREDENTIAL
        }

        return listOf(
            Authenticators.BIOMETRIC_STRONG,
            Authenticators.BIOMETRIC_WEAK,
            Authenticators.DEVICE_CREDENTIAL,
        )
            .filter { BiometricManager.from(context).canAuthenticate(it) == BiometricManager.BIOMETRIC_SUCCESS }
            .fold(0) { acc, auth -> acc or auth }
    }

    fun isSupported(context: Context): Boolean {
        return isLegacySecured(context) || getSupportedAuthenticators(context) != 0
    }

    fun isDeviceCredentialAllowed(context: Context): Boolean {
        return isLegacySecured(context) || (getSupportedAuthenticators(context) and Authenticators.DEVICE_CREDENTIAL != 0)
    }

    /**
     * Returns whether the device is secured with a PIN, pattern or password.
     */
    private fun isLegacySecured(context: Context): Boolean {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            if (context.keyguardManager.isDeviceSecure) {
                return true
            }
        }
        return false
    }
}
