package eu.kanade.tachiyomi.ui.reader.viewer.navigation

import android.graphics.RectF
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.util.lang.invert

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
class KindlishNavigation(invertHorizontal: Boolean = false, invertVertical: Boolean = false) : ViewerNavigation() {

    override var nextRegion = listOf(
        RectF(0.33f, 0.30f, 1f, 1f)
    ).map { it.invert(invertHorizontal, invertVertical) }

    override var prevRegion = listOf(
        RectF(0f, 0.30f, 0.33f, 1f)
    ).map { it.invert(invertHorizontal, invertVertical) }
}
