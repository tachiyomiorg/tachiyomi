package eu.kanade.tachiyomi.ui.browse.extension

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import eu.kanade.tachiyomi.extension.model.Extension

fun Extension.getApplicationIcon(context: Context): Drawable? {
    return try {
        context.packageManager.getApplicationIcon(pkgName)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
}
