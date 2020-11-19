package eu.kanade.tachiyomi.ui.reader.viewer.navigation

import android.graphics.RectF
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.util.lang.invert

/**
 * Visualization of default state without any inversion
 * +---+---+---+
 * | N | N | N |   P: Previous
 * +---+---+---+
 * | N | M | P |   M: Menu
 * +---+---+---+
 * | P | P | P |   N: Next
 * +---+---+---+
 */
open class LNavigation(invertHorizontal: Boolean = false, invertVertical: Boolean = false) : ViewerNavigation() {

    override var nextRegion = listOf(
        RectF(0.66f, 0.33f, 1f, 0.66f),
        RectF(0f, 0.66f, 1f, 1f)
    ).map { it.invert(invertHorizontal, invertVertical) }

    override var prevRegion = listOf(
        RectF(0f, 0.33f, 0.33f, 0.66f),
        RectF(0f, 0f, 1f, 0.33f)
    ).map { it.invert(invertHorizontal, invertVertical) }
}
