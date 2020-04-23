package eu.kanade.tachiyomi.ui.reader.viewer

import com.tfcporciuncula.flow.Preference
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.lang.addTo
import eu.kanade.tachiyomi.util.lang.launchInUI
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import rx.subscriptions.CompositeSubscription

abstract class ViewerConfig(preferences: PreferencesHelper) {

    private val subscriptions = CompositeSubscription()

    var imagePropertyChangedListener: (() -> Unit)? = null

    var tappingEnabled = true
    var longTapEnabled = true
    var volumeKeysEnabled = false
    var volumeKeysInverted = false
    var alwaysShowChapterTransition = true

    init {
        preferences.readWithTapping()
            .register({ tappingEnabled = it })

        preferences.readWithLongTap()
            .register({ longTapEnabled = it })

        preferences.readWithVolumeKeys()
            .register({ volumeKeysEnabled = it })

        preferences.readWithVolumeKeysInverted()
            .register({ volumeKeysInverted = it })

        preferences.alwaysShowChapterTransition()
            .register({ alwaysShowChapterTransition = it })
    }

    fun unsubscribe() {
        subscriptions.unsubscribe()
    }

    fun <T> com.f2prateek.rx.preferences.Preference<T>.register(
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

    fun <T> Preference<T>.register(
        valueAssignment: (T) -> Unit,
        onChanged: (T) -> Unit = {}
    ) {
        asFlow()
            .onEach { valueAssignment(it) }
            .distinctUntilChanged()
            .onEach { onChanged(it) }
            .launchInUI()
    }
}
