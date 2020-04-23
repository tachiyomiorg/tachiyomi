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

    var doubleTapAnimDuration = 500
        private set

    var sidePadding = 0
        private set

    init {
        preferences.readWithTapping()
            .register({ tappingEnabled = it })

        preferences.readWithLongTap()
            .register({ longTapEnabled = it })

        preferences.cropBordersWebtoon()
            .register({ imageCropBorders = it }, { imagePropertyChangedListener?.invoke() })

        preferences.doubleTapAnimSpeed()
            .register({ doubleTapAnimDuration = it })

        preferences.readWithVolumeKeys()
            .register({ volumeKeysEnabled = it })

        preferences.readWithVolumeKeysInverted()
            .register({ volumeKeysInverted = it })

        preferences.alwaysShowChapterTransition()
            .register({ alwaysShowChapterTransition = it })

        preferences.webtoonSidePadding()
            .register({ sidePadding = it }, { imagePropertyChangedListener?.invoke() })
    }
}
