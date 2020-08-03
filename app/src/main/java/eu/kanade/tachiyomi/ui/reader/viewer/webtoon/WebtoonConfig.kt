package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerConfig
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
    }
}
