package eu.kanade.tachiyomi.util

import android.graphics.Color
import android.graphics.Point
import android.support.design.widget.Snackbar
import android.view.View
import android.widget.TextView

/**
 * Returns coordinates of view.
 * Used for animation
 *
 * @return coordinates of view
 */
fun View.getCoordinates() = Point((left + right) / 2, (top + bottom) / 2)

/**
 * Shows a snackbar in this view.
 *
 * @param message the message to show.
 * @param length the duration of the snack.
 * @param f a function to execute in the snack, allowing for example to define a custom action.
 */
inline fun View.snack(message: String, length: Int = Snackbar.LENGTH_LONG, f: Snackbar.() -> Unit): Snackbar {
    val snack = Snackbar.make(this, message, length)
    val textView = snack.view.findViewById(android.support.design.R.id.snackbar_text) as TextView
    textView.setTextColor(Color.WHITE)
    snack.f()
    snack.show()
    return snack
}