package eu.kanade.tachiyomi.ui.reader.viewer.navigation

import android.graphics.RectF
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation

/**
 * Visualization of default state without any inversion
* +---+---+---+
* | M | M | M |   P: Previous
* +---+---+---+
* | P | N | N |   M: Menu
* +---+---+---+
* | P | N | N |   N: Next
* +---+---+---+
*/
class KindlishNavigation(invertHorizontal: Boolean = false) : ViewerNavigation() {

    override var nextRegion = listOf(
        when (invertHorizontal) {
            false -> RectF(0.33f, 0.30f, 1f, 1f)
            true -> RectF(0f, 0.30f, 0.33f, 1f)
        }
    )

    override var prevRegion = listOf(
        when (invertHorizontal) {
            true -> RectF(0.33f, 0.30f, 1f, 1f)
            false -> RectF(0f, 0.30f, 0.33f, 1f)
        }
    )
}
