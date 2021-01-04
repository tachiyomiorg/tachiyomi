package eu.kanade.tachiyomi.ui.reader.viewer

import android.graphics.PointF
import android.graphics.RectF
import eu.kanade.tachiyomi.data.preference.PreferenceValues
import eu.kanade.tachiyomi.util.lang.invert

abstract class ViewerNavigation {

    enum class NavigationRegion {
        NEXT, PREV, MENU, RIGHT, LEFT
    }

    data class Region(
        val rectF: RectF,
        val type: NavigationRegion
    ) {
        fun invert(invertMode: PreferenceValues.TappingInvertMode): Region {
            if (invertMode == PreferenceValues.TappingInvertMode.NONE) return this
            return this.copy(
                rectF = this.rectF.invert(invertMode)
            )
        }
    }

    private var constantMenuRegion: RectF = RectF(0f, 0f, 1f, 0.05f)

    abstract var regions: List<Region>

    var invertMode: PreferenceValues.TappingInvertMode = PreferenceValues.TappingInvertMode.NONE

    fun getAction(pos: PointF): NavigationRegion {
        val x = pos.x
        val y = pos.y
        val region = regions.map { it.invert(invertMode) }
            .find { it.rectF.contains(x, y) }
        return when {
            region != null -> region.type
            constantMenuRegion.contains(x, y) -> NavigationRegion.MENU
            else -> NavigationRegion.MENU
        }
    }
}
