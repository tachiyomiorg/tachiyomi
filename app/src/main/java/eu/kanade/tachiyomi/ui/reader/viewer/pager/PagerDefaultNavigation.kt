package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.graphics.RectF
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.LNavigation

/**
 * Visualization of default state without any inversion
 * +---+---+---+
 * | N | M | P |   P: Move Right
 * +---+---+---+
 * | N | M | P |   M: Menu
 * +---+---+---+
 * | N | M | P |   N: Move Left
 * +---+---+---+
 */
class PagerDefaultNavigation : ViewerNavigation() {

    override var regions: List<Region> = listOf(
        Region(
            rectF = RectF(0f, 0f, 0.33f, 1f),
            type = NavigationRegion.NEXT
        ),
        Region(
            rectF = RectF(0.66f, 0f, 1f, 1f),
            type = NavigationRegion.PREV
        ),
    )
}

class VerticalPagerDefaultNavigation : LNavigation()
