package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import com.davemorrissey.labs.subscaleview.decoder.*
import com.f2prateek.rx.preferences.Preference
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.addTo
import rx.subscriptions.CompositeSubscription
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class WebtoonConfig(preferences: PreferencesHelper = Injekt.get()) {

    private val subscriptions = CompositeSubscription()

    var imagePropertyChangedListener: (() -> Unit)? = null

    var tappingEnabled = true
        private set

    var volumeKeysEnabled = false
        private set

    var volumeKeysInverted = false
        private set

    var imageCropBorders = false
        private set

    var doubleTapAnimDuration = 500
        private set

    var bitmapDecoder: Class<out ImageDecoder> = IImageDecoder::class.java
        private set

    var regionDecoder: Class<out ImageRegionDecoder> = IImageRegionDecoder::class.java
        private set

    init {
        preferences.readWithTapping()
            .register({ tappingEnabled = it })

        preferences.cropBordersWebtoon()
            .register({ imageCropBorders = it }, { imagePropertyChangedListener?.invoke() })

        preferences.doubleTapAnimSpeed()
            .register({ doubleTapAnimDuration = it })

        preferences.imageDecoder()
            .register({ decoderFromPreference(it) })

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

    private fun decoderFromPreference(value: Int) {
        when (value) {
            // Image decoder
            0 -> {
                bitmapDecoder = IImageDecoder::class.java
                regionDecoder = IImageRegionDecoder::class.java
            }
            // Skia decoder
            2 -> {
                bitmapDecoder = SkiaImageDecoder::class.java
                regionDecoder = SkiaImageRegionDecoder::class.java
            }
        }
    }

}
