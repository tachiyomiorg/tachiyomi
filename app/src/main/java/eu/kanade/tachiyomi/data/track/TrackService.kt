package eu.kanade.tachiyomi.data.track

import android.support.annotation.CallSuper
import android.support.annotation.DrawableRes
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.network.NetworkHelper
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import okhttp3.OkHttpClient
import rx.Completable
import rx.Observable
import uy.kohesive.injekt.injectLazy

abstract class TrackService(val id: Int) {

    val preferences: PreferencesHelper by injectLazy()
    val networkService: NetworkHelper by injectLazy()

    open val client: OkHttpClient
        get() = networkService.client

    // Name of the manga sync service to display
    abstract val name: String

    abstract fun login(username: String, password: String): Completable

    open val isLogged: Boolean
        get() = !getUsername().isEmpty() &&
                !getPassword().isEmpty()

    abstract fun add(track: Track): Observable<Track>

    abstract fun update(track: Track): Observable<Track>

    abstract fun bind(track: Track): Observable<Track>

    abstract fun search(query: String): Observable<List<Track>>

    abstract fun refresh(track: Track): Observable<Track>

    abstract fun getStatus(status: Int): String

    abstract fun getStatusList(): List<Int>

    @DrawableRes
    abstract fun getLogo(): Int

    abstract fun getLogoColor(): Int

    // TODO better support (decimals)
    abstract fun maxScore(): Int

    abstract fun formatScore(track: Track): String

    fun saveCredentials(username: String, password: String) {
        preferences.setTrackCredentials(this, username, password)
    }

    @CallSuper
    open fun logout() {
        preferences.setTrackCredentials(this, "", "")
    }

    fun getUsername() = preferences.trackUsername(this)

    fun getPassword() = preferences.trackPassword(this)

}
