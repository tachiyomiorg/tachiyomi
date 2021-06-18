package eu.kanade.tachiyomi.util.view

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import eu.kanade.tachiyomi.util.system.InternalResourceHelper
import eu.kanade.tachiyomi.util.system.getResourceColor

fun Window.showBar() {
    decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
}

fun Window.hideBar() {
    decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
        View.SYSTEM_UI_FLAG_FULLSCREEN or
        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
}

fun Window.defaultBar() {
    decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
}

fun Window.isDefaultBar() = decorView.systemUiVisibility == View.SYSTEM_UI_FLAG_VISIBLE

/**
 * Sets navigation bar color to transparent if system's config_navBarNeedsScrim is false,
 * otherwise it will use the theme navigationBarColor with 70% opacity.
 */
fun Window.setNavigationBarTransparentCompat(context: Context) {
    navigationBarColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
        !InternalResourceHelper.getBoolean(context, "config_navBarNeedsScrim", true)
    ) {
        Color.TRANSPARENT
    } else {
        // Set navbar scrim 70% of navigationBarColor
        context.getResourceColor(android.R.attr.navigationBarColor, 0.7F)
    }
}

fun Window.setSecureScreen(enabled: Boolean) {
    if (enabled) {
        setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
    } else {
        clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}
