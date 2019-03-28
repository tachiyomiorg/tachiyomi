package eu.kanade.tachiyomi.ui.reader.viewer.pager

import com.f2prateek.rx.preferences.Preference
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.addTo
import rx.subscriptions.CompositeSubscription
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Configuration used by pager viewers.
 */
class PagerConfig(private val viewer: PagerViewer, preferences: PreferencesHelper = Injekt.get()) {

    private val subscriptions = CompositeSubscription()

    var imagePropertyChangedListener: (() -> Unit)? = null

    var tappingEnabled = true
        private set

    var longTapEnabled = true
        private set

    var volumeKeysEnabled = false
        private set

    var volumeKeysInverted = false
        private set

    var usePageTransitions = false
        private set

    var imageScaleType = 1
        private set

    var imageZoomType = ZoomType.Left
        private set

    var imageCropBorders = false
        private set

    var doubleTapAnimDuration = 500
        private set

    init {
        preferences.readWithTapping()
            .register({ tappingEnabled = it })

        preferences.readWithLongTap()
            .register({ longTapEnabled = it })

        preferences.pageTransitions()
            .register({ usePageTransitions = it })

        preferences.imageScaleType()
            .register({ imageScaleType = it }, { imagePropertyChangedListener?.invoke() })

        preferences.zoomStart()
            .register({ zoomTypeFromPreference(it) }, { imagePropertyChangedListener?.invoke() })

        preferences.cropBorders()
            .register({ imageCropBorders = it }, { imagePropertyChangedListener?.invoke() })

        preferences.doubleTapAnimSpeed()
            .register({ doubleTapAnimDuration = it })

        preferences.readWithVolumeKeys()
            .register({ volumeKeysEnabled = it })

        preferences.readWithVolumeKeysInverted()
            .register({ volumeKeysInverted = it })
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

    enum class ZoomType {
        Left, Center, Right
    }

}
