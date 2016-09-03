package eu.kanade.tachiyomi.data.mangasync

import android.content.Context
import android.support.annotation.CallSuper
import eu.kanade.tachiyomi.data.database.models.MangaSync
import eu.kanade.tachiyomi.data.network.NetworkHelper
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import okhttp3.OkHttpClient
import rx.Completable
import rx.Observable
import uy.kohesive.injekt.injectLazy

abstract class MangaSyncService(private val context: Context, val id: Int) {

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

    val statusMap by lazy {
        getStatusList().associate { it to getStatus(it) }
    }

    abstract fun add(manga: MangaSync): Observable<MangaSync>

    abstract fun update(manga: MangaSync): Observable<MangaSync>

    abstract fun bind(manga: MangaSync): Observable<MangaSync>

    abstract fun getStatus(status: Int): String

    abstract fun getList(): Observable<List<MangaSync>>

    abstract fun search(query: String): Observable<List<MangaSync>>

    abstract fun getStatusList(): List<Int>

    /**
     * Returns the average score of service
     * @return average score
     */
    abstract fun getRemoteScore(manga: MangaSync): Float

    /**
     * Returns the score of user
     * This function should only be used for showing information in view.
     * @return user score
     */
    open fun getUserScore(manga: MangaSync): Float {
        return manga.score
    }

    /**
     * Returns the code from the string
     * @param name name of status
     * @return status id from string
     */
    fun getStatus(name: String): Int {
        statusMap.forEach { if (it.value == name) return it.key }
        throw Exception("Unknown status")
    }

    /**
     * Returns the score of user used for service
     * This function should only be used to update service score.
     * @return user score
     */
    open fun getServiceUserScore(score: Int): Float {
        return score.toFloat()
    }

    fun saveCredentials(username: String, password: String) {
        preferences.setMangaSyncCredentials(this, username, password)
    }

    @CallSuper
    open fun logout() {
        preferences.setMangaSyncCredentials(this, "", "")
    }

    fun getUsername() = preferences.mangaSyncUsername(this)

    fun getPassword() = preferences.mangaSyncPassword(this)

}
