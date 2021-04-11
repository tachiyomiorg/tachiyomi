package eu.kanade.tachiyomi.util.system

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators

object BiometricUtil {

    fun getSupportedAuthenticators(context: Context): Int {
        return listOf(
            Authenticators.BIOMETRIC_STRONG,
            Authenticators.BIOMETRIC_WEAK,
            Authenticators.DEVICE_CREDENTIAL,
        )
            .filter { BiometricManager.from(context).canAuthenticate(it) == BiometricManager.BIOMETRIC_SUCCESS }
            .fold(0) { acc, auth -> acc or auth }
    }

    fun isSupported(context: Context): Boolean {
        return getSupportedAuthenticators(context) != 0
    }

    fun isDeviceCredentialAllowed(context: Context): Boolean {
        return getSupportedAuthenticators(context) and Authenticators.DEVICE_CREDENTIAL != 0
    }
}
