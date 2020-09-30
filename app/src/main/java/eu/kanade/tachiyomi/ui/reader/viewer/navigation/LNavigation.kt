package eu.kanade.tachiyomi.ui.reader.viewer.navigation

import android.graphics.RectF
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation

class LNavigation : ViewerNavigation() {

    override var nextRegion = listOf(
        RectF(0.66f, 0.33f, 1f, 0.66f),
        RectF(0.33f, 0.66f, 1f, 1f)
    )

    override var prevRegion = listOf(
        RectF(0f, 0.33f, 0.33f, 0.66f),
        RectF(0f, 0f, 1f, 0.33f)
    )
}
