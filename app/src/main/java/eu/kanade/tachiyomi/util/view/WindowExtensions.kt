package eu.kanade.tachiyomi.util.view

import android.view.View
import android.view.Window

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
