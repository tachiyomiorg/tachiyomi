package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.graphics.RectF
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.LNavigation

/**
 * +---+---+---+
 * | N | M | P |   P: Previous
 * +---+---+---+
 * | N | M | P |   M: Menu
 * +---+---+---+
 * | N | M | P |   N: Next
 * +---+---+---+
 */
class PagerDefaultNavigation(invertHorizontal: Boolean) : ViewerNavigation() {
    override var nextRegion = listOf(
        when (invertHorizontal) {
            false -> RectF(0.66f, 0f, 1f, 1f)
            true -> RectF(0f, 0f, 0.33f, 1f)
        }
    )
    override var prevRegion = listOf(
        when (invertHorizontal) {
            true -> RectF(0.66f, 0f, 1f, 1f)
            false -> RectF(0f, 0f, 0.33f, 1f)
        }
    )
}

class VerticalPagerDefaultNavigation(invertHorizontal: Boolean, invertVertical: Boolean) : LNavigation(invertHorizontal, invertVertical)
