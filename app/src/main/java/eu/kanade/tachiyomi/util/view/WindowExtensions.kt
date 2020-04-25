package eu.kanade.tachiyomi.util.view

import android.view.View
import android.view.Window

fun Window.showBar() {
    val uiFlags = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    decorView.systemUiVisibility = uiFlags
}

fun Window.hideBar() {
    val uiFlags = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
        View.SYSTEM_UI_FLAG_FULLSCREEN or
        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    decorView.systemUiVisibility = uiFlags
}

fun Window.defaultBar() {
    decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
}

fun Window.isDefaultBar() = decorView.systemUiVisibility == View.SYSTEM_UI_FLAG_VISIBLE
