package eu.kanade.tachiyomi.ui.reader.viewer

import android.graphics.PointF
import android.graphics.RectF

abstract class ViewerNavigation {

    enum class NavigationRegion {
        NEXT, PREV, MENU
    }

    private var constantMenuRegion: RectF = RectF(0f, 0f, 1f, 0.05f)

    abstract var nextRegion: List<RectF>

    abstract var prevRegion: List<RectF>

    fun getAction(pos: PointF): NavigationRegion {
        val x = pos.x
        val y = pos.y
        return when {
            constantMenuRegion.contains(x, y) -> NavigationRegion.MENU
            nextRegion.any { it.contains(x, y) } -> NavigationRegion.NEXT
            prevRegion.any { it.contains(x, y) } -> NavigationRegion.PREV
            else -> NavigationRegion.MENU
        }
    }
}
