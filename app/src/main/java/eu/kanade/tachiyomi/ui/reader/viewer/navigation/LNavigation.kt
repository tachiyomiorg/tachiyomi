package eu.kanade.tachiyomi.ui.reader.viewer.navigation

import android.graphics.RectF
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation

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
        when (invertHorizontal) {
            false -> RectF(0.66f, 0.33f, 1f, 0.66f)
            true -> RectF(0f, 0.33f, 0.33f, 0.66f)
        },
        when (invertVertical) {
            false -> RectF(0f, 0.66f, 1f, 1f)
            true -> RectF(0f, 0f, 1f, 0.33f)
        }
    )

    override var prevRegion = listOf(
        when (invertHorizontal) {
            true -> RectF(0.66f, 0.33f, 1f, 0.66f)
            false -> RectF(0f, 0.33f, 0.33f, 0.66f)
        },
        when (invertVertical) {
            true -> RectF(0f, 0.66f, 1f, 1f)
            false -> RectF(0f, 0f, 1f, 0.33f)
        }
    )
}
