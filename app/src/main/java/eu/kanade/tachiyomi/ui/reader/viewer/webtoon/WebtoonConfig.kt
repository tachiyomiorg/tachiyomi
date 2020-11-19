package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import eu.kanade.tachiyomi.data.preference.PreferenceValues.TappingInvertMode
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerConfig
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.KindlishNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.LNavigation
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Configuration used by webtoon viewers.
 */
class WebtoonConfig(preferences: PreferencesHelper = Injekt.get()) : ViewerConfig(preferences) {

    var imageCropBorders = false
        private set

    var sidePadding = 0
        private set

    override var navigator: ViewerNavigation = defaultViewerNavigation()

    init {
        preferences.cropBordersWebtoon()
            .register({ imageCropBorders = it }, { imagePropertyChangedListener?.invoke() })

        preferences.webtoonSidePadding()
            .register({ sidePadding = it }, { imagePropertyChangedListener?.invoke() })

        preferences.navigationModeWebtoon()
            .register({ navigationMode = it }, { viewerNavigation(it, tappingInverted) })
    }

    override fun defaultViewerNavigation(invertHorizontal: Boolean, invertVertical: Boolean): ViewerNavigation {
        return WebtoonDefaultNavigation(invertHorizontal, invertVertical)
    }

    override fun viewerNavigation(navigationMode: Int, invertMode: TappingInvertMode) {
        val invertHorizontal = invertMode.shouldInvertHorizontal()
        val invertVertical = invertMode.shouldInvertVertical()

        this.navigator = when (navigationMode) {
            0 -> defaultViewerNavigation(invertHorizontal, invertVertical)
            1 -> LNavigation(invertHorizontal, invertVertical)
            2 -> KindlishNavigation(invertHorizontal, invertVertical)
            else -> defaultViewerNavigation(invertHorizontal, invertVertical)
        }
    }
}
