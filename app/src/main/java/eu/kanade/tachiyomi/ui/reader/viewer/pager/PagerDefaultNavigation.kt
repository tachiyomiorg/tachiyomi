package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.graphics.RectF
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation

class PagerDefaultNavigation : ViewerNavigation() {
    override var nextRegion = listOf<RectF>(
        RectF(0.66f, 0f, 1f, 1f)
    )
    override var prevRegion = listOf<RectF>(
        RectF(0f, 0f, 0.33f, 1f)
    )
}
