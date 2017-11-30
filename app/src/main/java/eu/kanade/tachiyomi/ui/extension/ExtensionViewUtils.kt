package eu.kanade.tachiyomi.ui.extension

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.extension.model.Extension
import java.util.*

fun Extension.getLocalizedLang(context: Context): String {
    return when (lang) {
        null -> ""
        "" -> context.getString(R.string.other_source)
        "all" -> context.getString(R.string.all_lang)
        else -> {
            val locale = Locale(lang)
            locale.getDisplayName(locale).capitalize()
        }
    }
}

fun Extension.getApplicationIcon(context: Context): Drawable? {
    return try {
        context.packageManager.getApplicationIcon(pkgName)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
}
