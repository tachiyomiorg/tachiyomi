package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.graphics.RectF
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation

class WebtoonDefaultNavigation : ViewerNavigation() {

    override var nextRegion = listOf(
        RectF(0f, 0.66f, 1f, 1f)
    )

    override var prevRegion = listOf(
        RectF(0f, 0f, 1f, 0.33f)
    )
}
