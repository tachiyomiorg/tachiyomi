package eu.kanade.tachiyomi.ui.reader.viewer.navigation

import android.graphics.RectF
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation

/**
 * Visualization of default state without any inversion
 * +---+---+---+
 * | P | P | P |   P: Previous
 * +---+---+---+
 * | P | M | N |   M: Menu
 * +---+---+---+
 * | N | N | N |   N: Next
 * +---+---+---+
 */
open class LNavigation : ViewerNavigation() {

    override var regions: List<Region> = listOf(
        Region(
            rectF = RectF(0f, 0.33f, 0.33f, 0.66f),
            type = NavigationRegion.PREV
        ),
        Region(
            rectF = RectF(0f, 0f, 1f, 0.33f),
            type = NavigationRegion.PREV
        ),
        Region(
            rectF = RectF(0.66f, 0.33f, 1f, 0.66f),
            type = NavigationRegion.NEXT
        ),
        Region(
            rectF = RectF(0f, 0.66f, 1f, 1f),
            type = NavigationRegion.NEXT
        )
    )
}
