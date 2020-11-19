package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.graphics.RectF
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.LNavigation
import eu.kanade.tachiyomi.util.lang.invert

/**
 * Visualization of default state without any inversion
 * +---+---+---+
 * | N | M | P |   P: Previous
 * +---+---+---+
 * | N | M | P |   M: Menu
 * +---+---+---+
 * | N | M | P |   N: Next
 * +---+---+---+
 */
class PagerDefaultNavigation(invertHorizontal: Boolean = false) : ViewerNavigation() {

    override var nextRegion = listOf(
        RectF(0.66f, 0f, 1f, 1f)
    ).map { it.invert(invertHorizontal, false) }

    override var prevRegion = listOf(
        RectF(0f, 0f, 0.33f, 1f)
    ).map { it.invert(invertHorizontal, false) }
}

class VerticalPagerDefaultNavigation(invertHorizontal: Boolean = false, invertVertical: Boolean = false) : LNavigation(invertHorizontal, invertVertical)
