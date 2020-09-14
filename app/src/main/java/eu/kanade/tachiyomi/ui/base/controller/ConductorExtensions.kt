package eu.kanade.tachiyomi.ui.base.controller

import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import androidx.core.content.ContextCompat
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction

fun Router.popControllerWithTag(tag: String): Boolean {
    val controller = getControllerWithTag(tag)
    if (controller != null) {
        popController(controller)
        return true
    }
    return false
}

fun Controller.requestPermissionsSafe(permissions: Array<String>, requestCode: Int) {
    val activity = activity ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        permissions.forEach { permission ->
            if (ContextCompat.checkSelfPermission(activity, permission) != PERMISSION_GRANTED) {
                requestPermissions(arrayOf(permission), requestCode)
            }
        }
    }
}

fun Controller.withFadeTransaction(): RouterTransaction {
    return RouterTransaction.with(this)
        .pushChangeHandler(OneWayFadeChangeHandler())
        .popChangeHandler(OneWayFadeChangeHandler())
}
