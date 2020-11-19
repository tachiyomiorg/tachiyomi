package eu.kanade.tachiyomi.util.lang

import android.graphics.RectF

fun RectF.invert(horizontal: Boolean = false, vertical: Boolean = false): RectF {
    return when {
        horizontal && vertical -> RectF(1f - this.right, 1f - this.bottom, 1f - this.left, 1f - this.top)
        vertical -> RectF(this.left, 1f - this.bottom, this.right, 1f - this.top)
        horizontal -> RectF(1f - this.right, this.top, 1f - this.left, this.bottom)
        else -> this
    }
}
