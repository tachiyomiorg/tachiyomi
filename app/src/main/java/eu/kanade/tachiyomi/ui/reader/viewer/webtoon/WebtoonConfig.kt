package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

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

    init {
        preferences.cropBordersWebtoon()
            .register({ imageCropBorders = it }, { imagePropertyChangedListener?.invoke() })

        preferences.webtoonSidePadding()
            .register({ sidePadding = it }, { imagePropertyChangedListener?.invoke() })

        preferences.navigationModeWebtoon()
            .register({ navigationMode = it }, { viewerNavigation() })
    }

    override fun defaultViewerNavigation(): ViewerNavigation {
        return WebtoonDefaultNavigation()
    }

    override fun viewerNavigation() {
        this.navigator = when (navigationMode) {
            0 -> defaultViewerNavigation()
            1 -> LNavigation()
            2 -> KindlishNavigation()
            else -> defaultViewerNavigation()
        }
        super.viewerNavigation()
    }
}
