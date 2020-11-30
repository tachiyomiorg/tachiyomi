package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.graphics.RectF
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.LNavigation

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
class PagerDefaultNavigation : ViewerNavigation() {

    override var invertVertical: Boolean = false
        set(_) { field = false }

    override var nextRegion = navigationOf(
        RectF(0f, 0f, 0.33f, 1f)
    )

    override var prevRegion = navigationOf(
        RectF(0.66f, 0f, 1f, 1f)
    )
}

class VerticalPagerDefaultNavigation : LNavigation()
