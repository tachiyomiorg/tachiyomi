package eu.kanade.tachiyomi.data.track.anilist

import android.content.Context
import android.graphics.Color
import com.github.salomonbrys.kotson.int
import com.github.salomonbrys.kotson.string
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.data.track.TrackService
import rx.Completable
import rx.Observable
import timber.log.Timber

class Anilist(private val context: Context, id: Int) : TrackService(id) {

    companion object {
        const val READING = 1
        const val COMPLETED = 2
        const val ON_HOLD = 3
        const val DROPPED = 4
        const val PLAN_TO_READ = 5

        const val DEFAULT_STATUS = READING
        const val DEFAULT_SCORE = 0
    }

    override val name = "AniList"

    private val interceptor by lazy { AnilistInterceptor(getPassword()) }

    private val api by lazy {
        AnilistApi.createService(networkService.client.newBuilder()
                .addInterceptor(interceptor)
                .build())
    }

    override fun getLogo() = R.drawable.al

    override fun getLogoColor() = Color.rgb(18, 25, 35)

    override fun maxScore() = 100

    override fun login(username: String, password: String) = login(password)

    fun login(authCode: String): Completable {
        // Create a new api with the default client to avoid request interceptions.
        return AnilistApi.createService(client)
                // Request the access token from the API with the authorization code.
                .requestAccessToken(authCode)
                // Save the token in the interceptor.
                .doOnNext { interceptor.setAuth(it) }
                // Obtain the authenticated user from the API.
                .zipWith(api.getCurrentUser().map {
                    preferences.anilistScoreType().set(it["score_type"].int)
                    it["id"].string
                }, { oauth, user -> Pair(user, oauth.refresh_token!!) })
                // Save service credentials (username and refresh token).
                .doOnNext { saveCredentials(it.first, it.second) }
                // Logout on any error.
                .doOnError { logout() }
                .toCompletable()
    }

    override fun logout() {
        super.logout()
        interceptor.setAuth(null)
    }

    override fun search(query: String): Observable<List<Track>> {
        return api.search(query, 1)
                .flatMap { Observable.from(it) }
                .filter { it.type != "Novel" }
                .map { it.toTrack() }
                .toList()
    }

    fun getList(): Observable<List<Track>> {
        return api.getList(getUsername())
                .flatMap { Observable.from(it.flatten()) }
                .map { it.toTrack() }
                .toList()
    }

    override fun add(track: Track): Observable<Track> {
        return api.addManga(track.remote_id, track.last_chapter_read, track.getAnilistStatus())
                .doOnNext { it.body().close() }
                .doOnNext { if (!it.isSuccessful) throw Exception("Could not add manga") }
                .doOnError { Timber.e(it) }
                .map { track }
    }

    override fun update(track: Track): Observable<Track> {
        if (track.total_chapters != 0 && track.last_chapter_read == track.total_chapters) {
            track.status = COMPLETED
        }
        return api.updateManga(track.remote_id, track.last_chapter_read, track.getAnilistStatus(),
                track.getAnilistScore())
                .doOnNext { it.body().close() }
                .doOnNext { if (!it.isSuccessful) throw Exception("Could not update manga") }
                .doOnError { Timber.e(it) }
                .map { track }
    }

    override fun bind(track: Track): Observable<Track> {
        return getList()
                .flatMap { userlist ->
                    track.sync_id = id
                    val remoteTrack = userlist.find { it.remote_id == track.remote_id }
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

    override fun refresh(track: Track): Observable<Track> {
        return getList()
                .map { myList ->
                    val remoteTrack = myList.find { it.remote_id == track.remote_id }
                    if (remoteTrack != null) {
                        track.copyPersonalFrom(remoteTrack)
                        track.total_chapters = remoteTrack.total_chapters
                        track
                    } else {
                        throw Exception("Could not find manga")
                    }
                }
    }

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

    private fun Track.getAnilistStatus() = when (status) {
        READING -> "reading"
        COMPLETED -> "completed"
        ON_HOLD -> "on-hold"
        DROPPED -> "dropped"
        PLAN_TO_READ -> "plan to read"
        else -> throw NotImplementedError("Unknown status")
    }

    fun Track.getAnilistScore(): String = when (preferences.anilistScoreType().getOrDefault()) {
        // 10 point
        0 -> Math.floor(score.toDouble() / 10).toInt().toString()
        // 100 point
        1 -> score.toInt().toString()
        // 5 stars
        2 -> when {
            score == 0f -> "0"
            score < 30 -> "1"
            score < 50 -> "2"
            score < 70 -> "3"
            score < 90 -> "4"
            else -> "5"
        }
        // Smiley
        3 -> when {
            score == 0f -> "0"
            score <= 30 -> ":("
            score <= 60 -> ":|"
            else -> ":)"
        }
        // 10 point decimal
        4 -> (score / 10).toString()
        else -> throw Exception("Unknown score type")
    }

    override fun formatScore(track: Track): String {
        return track.getAnilistScore()
    }

}

