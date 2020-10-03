package eu.kanade.tachiyomi.ui.reader.viewer.pager

import eu.kanade.tachiyomi.data.preference.PreferenceValues.TappingInvertMode
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerConfig
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.KindlishNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.LNavigation
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Configuration used by pager viewers.
 */
class PagerConfig(private val viewer: PagerViewer, preferences: PreferencesHelper = Injekt.get()) :
    ViewerConfig(preferences) {

    var imageScaleType = 1
        private set

    var imageZoomType = ZoomType.Left
        private set

    var imageCropBorders = false
        private set

    override var navigator: ViewerNavigation = defaultViewerNavigation()

    init {
        preferences.imageScaleType()
            .register({ imageScaleType = it }, { imagePropertyChangedListener?.invoke() })

        preferences.zoomStart()
            .register({ zoomTypeFromPreference(it) }, { imagePropertyChangedListener?.invoke() })

        preferences.cropBorders()
            .register({ imageCropBorders = it }, { imagePropertyChangedListener?.invoke() })

        preferences.navigationModePager()
            .register({ navigationMode = it }, { viewerNavigation(it, tappingInverted) })
    }

    private fun zoomTypeFromPreference(value: Int) {
        imageZoomType = when (value) {
            // Auto
            1 -> when (viewer) {
                is L2RPagerViewer -> ZoomType.Left
                is R2LPagerViewer -> ZoomType.Right
                else -> ZoomType.Center
            }
            // Left
            2 -> ZoomType.Left
            // Right
            3 -> ZoomType.Right
            // Center
            else -> ZoomType.Center
        }
    }

    override fun defaultViewerNavigation(invertHorizontal: Boolean, invertVertical: Boolean): ViewerNavigation {
        return when (viewer) {
            is VerticalPagerViewer -> VerticalPagerDefaultNavigation(invertHorizontal, invertVertical)
            else -> PagerDefaultNavigation(invertHorizontal)
        }
    }

    override fun viewerNavigation(navigationMode: Int, invertMode: TappingInvertMode) {
        val invertHorizontal = invertMode.shouldInvertHorizontal()
        val invertVertical = invertMode.shouldInvertVertical()

        navigator = when (navigationMode) {
            0 -> defaultViewerNavigation(invertHorizontal, invertVertical)
            1 -> LNavigation(invertHorizontal, invertVertical)
            2 -> KindlishNavigation()

            else -> defaultViewerNavigation(invertHorizontal, invertVertical)
        }
    }

    enum class ZoomType {
        Left, Center, Right
    }
}
