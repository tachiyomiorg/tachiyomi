package eu.kanade.tachiyomi.data.track

import androidx.annotation.CallSuper
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.network.NetworkHelper
import okhttp3.OkHttpClient
import rx.Observable
import uy.kohesive.injekt.injectLazy

abstract class TrackService(val id: Int) {

    val preferences: PreferencesHelper by injectLazy()
    val networkService: NetworkHelper by injectLazy()

    open val client: OkHttpClient
        get() = networkService.client

    // Name of the manga sync service to display
    abstract val name: String

    @DrawableRes
    abstract fun getLogo(): Int

    @ColorInt
    abstract fun getLogoColor(): Int

    abstract fun getStatusList(): List<Int>

    abstract fun getStatus(status: Int): String

    abstract fun getCompletionStatus(): Int

    abstract fun getScoreList(): List<String>

    open fun indexToScore(index: Int): Float {
        return index.toFloat()
    }

    abstract fun displayScore(track: Track): String

    abstract suspend fun add(track: Track): Track

    abstract suspend fun update(track: Track): Track

    abstract fun bind(track: Track): Observable<Track>

    abstract fun search(query: String): Observable<List<TrackSearch>>

    abstract fun refresh(track: Track): Observable<Track>

    abstract suspend fun login(username: String, password: String)

    @CallSuper
    open fun logout() {
        preferences.setTrackCredentials(this, "", "")
    }

    open val isLogged: Boolean
        get() = getUsername().isNotEmpty() &&
            getPassword().isNotEmpty()

    fun getUsername() = preferences.trackUsername(this)!!

    fun getPassword() = preferences.trackPassword(this)!!

    fun saveCredentials(username: String, password: String) {
        preferences.setTrackCredentials(this, username, password)
    }
}
