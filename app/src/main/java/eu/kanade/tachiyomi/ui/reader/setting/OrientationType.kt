package eu.kanade.tachiyomi.ui.reader.setting

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.lang.next

enum class OrientationType(val prefValue: Int, val flag: Int, @StringRes val stringRes: Int, @DrawableRes val iconRes: Int) {
    FREE(1, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, R.string.rotation_free, R.drawable.ic_screen_rotation_24dp),
    LOCKED_PORTRAIT(2, ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT, R.string.rotation_lock, R.drawable.ic_screen_lock_rotation_24dp),
    LOCKED_LANDSCAPE(2, ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, R.string.rotation_lock, R.drawable.ic_screen_lock_rotation_24dp),
    PORTRAIT(3, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, R.string.rotation_force_portrait, R.drawable.ic_screen_lock_portrait_24dp),
    LANDSCAPE(4, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, R.string.rotation_force_landscape, R.drawable.ic_screen_lock_landscape_24dp);

    companion object {
        fun fromPreference(preference: Int, resources: Resources): OrientationType = when (preference) {
            2 -> {
                val currentOrientation = resources.configuration.orientation
                if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
                    LOCKED_PORTRAIT
                } else {
                    LOCKED_LANDSCAPE
                }
            }
            3 -> PORTRAIT
            4 -> LANDSCAPE
            else -> FREE
        }

        fun getNextOrientation(preference: Int, resources: Resources): OrientationType {
            val current = if (preference == 2) {
                // Avoid issue due to 2 types having the same prefValue
                LOCKED_LANDSCAPE
            } else {
                fromPreference(preference, resources)
            }
            return current.next()
        }
    }
}
