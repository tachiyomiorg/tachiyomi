package eu.kanade.tachiyomi.data.track.myanimelist

import android.content.Context
import android.graphics.Color
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import rx.Completable
import rx.Observable

class MyAnimeList(private val context: Context, id: Int) : TrackService(id) {

    companion object {
        const val READING = 1
        const val COMPLETED = 2
        const val ON_HOLD = 3
        const val DROPPED = 4
        const val PLAN_TO_READ = 6

        const val DEFAULT_STATUS = READING
        const val DEFAULT_SCORE = 0

        const val BASE_URL = "https://myanimelist.net"
        const val USER_SESSION_COOKIE = "MALSESSIONID"
        const val LOGGED_IN_COOKIE = "is_logged_in"
    }

    private val interceptor by lazy { MyAnimeListInterceptor(this) }
    private val api by lazy { MyAnimeListApi(client, interceptor) }

    override val name: String
        get() = "MyAnimeList"

    override val supportsReadingDates: Boolean = true

    override fun getLogo() = R.drawable.ic_tracker_mal

    override fun getLogoColor() = Color.rgb(46, 81, 162)

    override fun getStatusList(): List<Int> {
        return listOf(READING, COMPLETED, ON_HOLD, DROPPED, PLAN_TO_READ)
    }

    override fun getStatus(status: Int): String = with(context) {
        when (status) {
            READING -> getString(R.string.reading)
            COMPLETED -> getString(R.string.completed)
            ON_HOLD -> getString(R.string.on_hold)
            DROPPED -> getString(R.string.dropped)
            PLAN_TO_READ -> getString(R.string.plan_to_read)
            else -> ""
        }
    }

    override fun getCompletionStatus(): Int = COMPLETED

    override fun getScoreList(): List<String> {
        return IntRange(0, 10).map(Int::toString)
    }

    override fun displayScore(track: Track): String {
        return track.score.toInt().toString()
    }

    override fun add(track: Track): Observable<Track> {
        return api.addLibManga(track)
    }

    override fun update(track: Track): Observable<Track> {
        return api.updateLibManga(track)
    }

    override fun bind(track: Track): Observable<Track> {
        return api.findLibManga(track)
            .flatMap { remoteTrack ->
                if (remoteTrack != null) {
                    track.copyPersonalFrom(remoteTrack)
                    update(track)
                } else {
                    // Set default fields if it's not found in the list
                    track.score = DEFAULT_SCORE.toFloat()
                    track.status = DEFAULT_STATUS
                    add(track)
                }
            }
    }

    override fun search(query: String): Observable<List<TrackSearch>> {
        return api.search(query)
    }

    override fun refresh(track: Track): Observable<Track> {
        return api.getLibManga(track)
            .map { remoteTrack ->
                track.copyPersonalFrom(remoteTrack)
                track.total_chapters = remoteTrack.total_chapters
                track
            }
    }

    fun login(csrfToken: String): Completable = login("myanimelist", csrfToken)

    override fun login(username: String, password: String): Completable {
        return Observable.fromCallable { saveCSRF(password) }
            .doOnNext { saveCredentials(username, password) }
            .doOnError { logout() }
            .toCompletable()
    }

    fun ensureLoggedIn() {
        if (isAuthorized) return
        if (!isLogged) throw Exception(context.getString(R.string.myanimelist_creds_missing))
    }

    override fun logout() {
        super.logout()
        preferences.trackToken(this).delete()
        networkService.cookieManager.remove(BASE_URL.toHttpUrlOrNull()!!)
    }

    private val isAuthorized: Boolean
        get() = super.isLogged &&
            getCSRF().isNotEmpty() &&
            checkCookies()

    fun getCSRF(): String = preferences.trackToken(this).get()

    private fun saveCSRF(csrf: String) = preferences.trackToken(this).set(csrf)

    private fun checkCookies(): Boolean {
        val url = BASE_URL.toHttpUrlOrNull()!!
        val ckCount = networkService.cookieManager.get(url).count {
            it.name == USER_SESSION_COOKIE || it.name == LOGGED_IN_COOKIE
        }

        return ckCount == 2
    }
}
