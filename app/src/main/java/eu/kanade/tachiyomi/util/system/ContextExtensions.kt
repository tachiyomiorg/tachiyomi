package eu.kanade.tachiyomi.util.system

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.PowerManager
import android.view.View
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.net.toUri
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.nononsenseapps.filepicker.FilePickerActivity
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.lang.truncateCenter
import eu.kanade.tachiyomi.widget.CustomLayoutPickerActivity
import kotlin.math.roundToInt

/**
 * Display a toast in this context.
 *
 * @param resource the text resource.
 * @param duration the duration of the toast. Defaults to short.
 */
fun Context.toast(@StringRes resource: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, resource, duration).show()
}

/**
 * Display a toast in this context.
 *
 * @param text the text to display.
 * @param duration the duration of the toast. Defaults to short.
 */
fun Context.toast(text: String?, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, text.orEmpty(), duration).show()
}

/**
 * Copies a string to clipboard
 *
 * @param label Label to show to the user describing the content
 * @param content the actual text to copy to the board
 */
fun Context.copyToClipboard(label: String, content: String) {
    if (content.isBlank()) return

    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, content))

    toast(getString(R.string.copied_to_clipboard, content.truncateCenter(50)))
}

/**
 * Helper method to create a notification builder.
 *
 * @param id the channel id.
 * @param block the function that will execute inside the builder.
 * @return a notification to be displayed or updated.
 */
fun Context.notificationBuilder(channelId: String, block: (NotificationCompat.Builder.() -> Unit)? = null): NotificationCompat.Builder {
    val builder = NotificationCompat.Builder(this, channelId)
        .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
    if (block != null) {
        builder.block()
    }
    return builder
}

/**
 * Helper method to create a notification.
 *
 * @param id the channel id.
 * @param block the function that will execute inside the builder.
 * @return a notification to be displayed or updated.
 */
fun Context.notification(channelId: String, block: (NotificationCompat.Builder.() -> Unit)?): Notification {
    val builder = notificationBuilder(channelId, block)
    return builder.build()
}

/**
 * Helper method to construct an Intent to use a custom file picker.
 * @param currentDir the path the file picker will open with.
 * @return an Intent to start the file picker activity.
 */
fun Context.getFilePicker(currentDir: String): Intent {
    return Intent(this, CustomLayoutPickerActivity::class.java)
        .putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)
        .putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true)
        .putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_DIR)
        .putExtra(FilePickerActivity.EXTRA_START_PATH, currentDir)
}

/**
 * Checks if the give permission is granted.
 *
 * @param permission the permission to check.
 * @return true if it has permissions.
 */
fun Context.hasPermission(permission: String) = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

/**
 * Returns the color for the given attribute.
 *
 * @param resource the attribute.
 * @param alphaFactor the alpha number [0,1].
 */
@ColorInt fun Context.getResourceColor(@AttrRes resource: Int, alphaFactor: Float = 1f): Int {
    val typedArray = obtainStyledAttributes(intArrayOf(resource))
    val color = typedArray.getColor(0, 0)
    typedArray.recycle()

    if (alphaFactor < 1f) {
        val alpha = (color.alpha * alphaFactor).roundToInt()
        return Color.argb(alpha, color.red, color.green, color.blue)
    }

    return color
}

/**
 * Converts to dp.
 */
val Int.pxToDp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()

/**
 * Converts to px.
 */
val Int.dpToPx: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

/**
 * Converts to px and takes into account LTR/RTL layout.
 */
val Float.dpToPxEnd: Float
    get() = (
        this * Resources.getSystem().displayMetrics.density *
            if (Resources.getSystem().isLTR) 1 else -1
        )

val Resources.isLTR
    get() = configuration.layoutDirection == View.LAYOUT_DIRECTION_LTR

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

/**
 * Convenience method to acquire a partial wake lock.
 */
fun Context.acquireWakeLock(tag: String): PowerManager.WakeLock {
    val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$tag:WakeLock")
    wakeLock.acquire()
    return wakeLock
}

/**
 * Function used to send a local broadcast asynchronous
 *
 * @param intent intent that contains broadcast information
 */
fun Context.sendLocalBroadcast(intent: Intent) {
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
}

/**
 * Function used to send a local broadcast synchronous
 *
 * @param intent intent that contains broadcast information
 */
fun Context.sendLocalBroadcastSync(intent: Intent) {
    LocalBroadcastManager.getInstance(this).sendBroadcastSync(intent)
}

/**
 * Function used to register local broadcast
 *
 * @param receiver receiver that gets registered.
 */
fun Context.registerLocalReceiver(receiver: BroadcastReceiver, filter: IntentFilter) {
    LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter)
}

/**
 * Function used to unregister local broadcast
 *
 * @param receiver receiver that gets unregistered.
 */
fun Context.unregisterLocalReceiver(receiver: BroadcastReceiver) {
    LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
}

/**
 * Returns true if the given service class is running.
 */
fun Context.isServiceRunning(serviceClass: Class<*>): Boolean {
    val className = serviceClass.name
    val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    @Suppress("DEPRECATION")
    return manager.getRunningServices(Integer.MAX_VALUE)
        .any { className == it.service.className }
}

/**
 * Opens a URL in a custom tab.
 */
fun Context.openInBrowser(url: String) {
    try {
        val intent = CustomTabsIntent.Builder()
            .setToolbarColor(getResourceColor(R.attr.colorPrimary))
            .build()
        intent.launchUrl(this, url.toUri())
    } catch (e: Exception) {
        toast(e.message)
    }
}
