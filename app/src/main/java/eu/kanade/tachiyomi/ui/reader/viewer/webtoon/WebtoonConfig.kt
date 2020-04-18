package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import com.f2prateek.rx.preferences.Preference
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.lang.addTo
import rx.subscriptions.CompositeSubscription
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerConfig
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Configuration used by webtoon viewers.
 */

    private val subscriptions = CompositeSubscription()
class WebtoonConfig(preferences: PreferencesHelper = Injekt.get()) : ViewerConfig() {

    var tappingEnabled = true
        private set

    var longTapEnabled = true
        private set

    var volumeKeysEnabled = false
        private set

    var volumeKeysInverted = false
        private set

    var imageCropBorders = false
        private set

    var doubleTapAnimDuration = 500
        private set

    var alwaysShowChapterTransition = true
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

    fun unsubscribe() {
        subscriptions.unsubscribe()
    }

    private fun <T> Preference<T>.register(
        valueAssignment: (T) -> Unit,
        onChanged: (T) -> Unit = {}
    ) {
        asObservable()
                .doOnNext(valueAssignment)
                .skip(1)
                .distinctUntilChanged()
                .doOnNext(onChanged)
                .subscribe()
                .addTo(subscriptions)
    }
}
