package eu.kanade.tachiyomi.data.track.anilist

import android.content.Context
import android.graphics.Color
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import rx.Completable
import rx.Observable

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

    private val api by lazy { AnilistApi(client, interceptor) }

    override fun getLogo() = R.drawable.al

    override fun getLogoColor() = Color.rgb(18, 25, 35)

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

    override fun getScoreList(): List<String> {
        return when (preferences.anilistScoreType().getOrDefault()) {
            // 10 point
            0 -> IntRange(0, 10).map(Int::toString)
            // 100 point
            1 -> IntRange(0, 100).map(Int::toString)
            // 5 stars
            2 -> IntRange(0, 5).map { "$it ★" }
            // Smiley
            3 -> listOf("-", "😦", "😐", "😊")
            // 10 point decimal
            4 -> IntRange(0, 100).map { (it / 10f).toString() }
            else -> throw Exception("Unknown score type")
        }
    }

    override fun indexToScore(index: Int): Float {
        return when (preferences.anilistScoreType().getOrDefault()) {
            // 10 point
            0 -> index * 10f
            // 100 point
            1 -> index.toFloat()
            // 5 stars
            2 -> index * 20f
            // Smiley
            3 -> index * 30f
            // 10 point decimal
            4 -> index.toFloat()
            else -> throw Exception("Unknown score type")
        }
    }

    override fun displayScore(track: Track): String {
        val score = track.score
        return when (preferences.anilistScoreType().getOrDefault()) {
            2 -> "${(score / 20).toInt()} ★"
            3 -> when {
                score == 0f -> "0"
                score <= 30 -> "😦"
                score <= 60 -> "😐"
                else -> "😊"
            }
            else -> track.toAnilistScore()
        }
    }

    override fun add(track: Track): Observable<Track> {
        return api.addLibManga(track)
    }

    override fun update(track: Track): Observable<Track> {
        if (track.total_chapters != 0 && track.last_chapter_read == track.total_chapters) {
            track.status = COMPLETED
        }

        return api.updateLibManga(track)
    }

    override fun bind(track: Track): Observable<Track> {
        return api.findLibManga(track, getUsername())
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
        return api.getLibManga(track, getUsername())
                .map { remoteTrack ->
                    track.copyPersonalFrom(remoteTrack)
                    track.total_chapters = remoteTrack.total_chapters
                    track
                }
    }

    override fun login(username: String, password: String) = login(password)

    fun login(authCode: String): Completable {
        return api.login(authCode)
                // Save the token in the interceptor.
                .doOnNext { interceptor.setAuth(it) }
                // Obtain the authenticated user from the API.
                .zipWith(api.getCurrentUser().map { pair ->
                    preferences.anilistScoreType().set(pair.second)
                    pair.first
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

}

