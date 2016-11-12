package eu.kanade.tachiyomi.util

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.PowerManager
import android.support.annotation.StringRes
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.widget.Toast

/**
 * Display a toast in this context.
 * @param resource the text resource.
 * @param duration the duration of the toast. Defaults to short.
 */
fun Context.toast(@StringRes resource: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, resource, duration).show()
}

/**
 * Display a toast in this context.
 * @param text the text to display.
 * @param duration the duration of the toast. Defaults to short.
 */
fun Context.toast(text: String?, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, text, duration).show()
}

/**
 * Helper method to create a notification.
 * @param func the function that will execute inside the builder.
 * @return a notification to be displayed or updated.
 */
inline fun Context.notification(func: NotificationCompat.Builder.() -> Unit): Notification {
    val builder = NotificationCompat.Builder(this)
    builder.func()
    return builder.build()
}

/**
 * Checks if the give permission is granted.
 * @param permission the permission to check.
 * @return true if it has permissions.
 */
fun Context.hasPermission(permission: String)
        = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

/**
 * Property to get the notification manager from the context.
 */
val Context.notificationManager: NotificationManager
    get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

/**
 * Property to get the connectivity manager from the context.
 */
val Context.connectivityManager: ConnectivityManager
    get() = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

/**
 * Property to get the power manager from the context.
 */
val Context.powerManager: PowerManager
    get() = getSystemService(Context.POWER_SERVICE) as PowerManager