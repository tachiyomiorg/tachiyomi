package eu.kanade.tachiyomi.util

import androidx.annotation.LayoutRes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

/**
 * Extension method to inflate a view directly from its parent.
 * @param layout the layout to inflate.
 * @param attachToRoot whether to attach the view to the root or not. Defaults to false.
 */
fun ViewGroup.inflate(@LayoutRes layout: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(context).inflate(layout, this, attachToRoot)
}
