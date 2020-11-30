package eu.kanade.tachiyomi.ui.reader.viewer

import android.graphics.PointF
import android.graphics.RectF
import eu.kanade.tachiyomi.util.lang.invert

abstract class ViewerNavigation {

    enum class NavigationRegion {
        NEXT, PREV, MENU
    }

    private var constantMenuRegion: RectF = RectF(0f, 0f, 1f, 0.05f)

    private var lastInvertHorizontal = false
    open var invertHorizontal: Boolean = false
        set(value) {
            lastInvertHorizontal = field
            field = value
        }

    private var lastInvertedVertical = false
    open var invertVertical: Boolean = false
        set(value) {
            lastInvertedVertical = field
            field = value
        }

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

    fun invert() {
        nextRegion = navigationOf(nextRegion)
        prevRegion = navigationOf(prevRegion)
    }

    protected fun navigationOf(elements: List<RectF>): List<RectF> {
        return navigationOf(*elements.toTypedArray())
    }

    /*
    * Inverts/Resets elements in the array based on what get turn on or off 
    */
    protected fun navigationOf(vararg elements: RectF): List<RectF> {
        val wasHorizontalInversionChanged = invertHorizontal != lastInvertHorizontal
        val wasVerticalInversionChanged = invertVertical != lastInvertedVertical
        return elements.map { it.invert(wasHorizontalInversionChanged, wasVerticalInversionChanged) }
    }
}
