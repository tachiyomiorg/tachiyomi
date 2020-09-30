package eu.kanade.tachiyomi.ui.reader.viewer.navigation

import android.graphics.RectF
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation

class KindlishNavigation : ViewerNavigation() {

    override var nextRegion = listOf(
        RectF(0.33f, 0.30f, 1f, 1f)
    )

    override var prevRegion = listOf(
        RectF(0f, 0.30f, 0.33f, 1f)
    )
}
