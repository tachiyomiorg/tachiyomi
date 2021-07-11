package eu.kanade.tachiyomi.util.system

import android.annotation.SuppressLint
import timber.log.Timber

object MiuiUtil {

    fun isMiui(): Boolean {
        return getSystemProperty("ro.miui.ui.version.name")?.isNotEmpty() ?: false
    }

    @SuppressLint("PrivateApi")
    fun isMiuiOptimizationDisabled(): Boolean {
        if ("0" == getSystemProperty("persist.sys.miui_optimization")) {
            return true
        }

        return try {
            Class.forName("android.miui.AppOpsUtils")
                .getDeclaredMethod("isXOptMode")
                .invoke(null) as Boolean
        } catch (e: Exception) {
            false
        }
    }

    @SuppressLint("PrivateApi")
    private fun getSystemProperty(key: String?): String? {
        return try {
            Class.forName("android.os.SystemProperties")
                .getDeclaredMethod("get", String::class.java)
                .invoke(null, key) as String
        } catch (e: Exception) {
            Timber.w(e, "Unable to use SystemProperties.get")
            null
        }
    }
}
