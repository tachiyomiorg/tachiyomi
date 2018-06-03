package eu.kanade.tachiyomi.data.track.anilist

import android.content.Context
import android.graphics.Color
import com.google.gson.Gson
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import rx.Completable
import rx.Observable
import uy.kohesive.injekt.injectLazy

class Anilist(private val context: Context, id: Int) : TrackService(id) {

    companion object {
        const val READING = 1
        const val COMPLETED = 2
        const val ON_HOLD = 3
        const val DROPPED = 4
        const val PLANNING = 5
        const val REPEATING = 6

        const val DEFAULT_STATUS = READING
        const val DEFAULT_SCORE = 0

        const val POINT_100 = "POINT_100"
        const val POINT_10 = "POINT_10"
        const val POINT_10_DECIMAL = "POINT_10_DECIMAL"
        const val POINT_5 = "POINT_5"
        const val POINT_3 = "POINT_3"
    }

    override val name = "AniList"

    private val gson: Gson by injectLazy()

    private val interceptor by lazy { AnilistInterceptor(this, getPassword()) }

    private val api by lazy { AnilistApi(client, interceptor) }

    private val scorePreference = preferences.anilistScoreType()

    init {
        // If the preference is an int from APIv1, logout user to force using APIv2
        try {
            scorePreference.get()
        } catch (e: ClassCastException) {
            logout()
            scorePreference.delete()
        }
    }

    override fun getLogo() = R.drawable.al

    override fun getLogoColor() = Color.rgb(18, 25, 35)

    override fun getStatusList(): List<Int> {
        return listOf(READING, COMPLETED, ON_HOLD, DROPPED, PLANNING, REPEATING)
    }

    override fun getStatus(status: Int): String = with(context) {
        when (status) {
            READING -> getString(R.string.reading)
            COMPLETED -> getString(R.string.completed)
            ON_HOLD -> getString(R.string.on_hold)
            DROPPED -> getString(R.string.dropped)
            PLANNING -> getString(R.string.plan_to_read)
            REPEATING -> getString(R.string.repeating)
            else -> ""
        }
    }

    override fun getScoreList(): List<String> {
        return when (scorePreference.getOrDefault()) {
            // 10 point
            POINT_10 -> IntRange(0, 10).map(Int::toString)
            // 100 point
            POINT_100 -> IntRange(0, 100).map(Int::toString)
            // 5 stars
            POINT_5 -> IntRange(0, 5).map { "$it ★" }
            // Smiley
            POINT_3 -> listOf("-", "😦", "😐", "😊")
            // 10 point decimal
            POINT_10_DECIMAL -> IntRange(0, 100).map { (it / 10f).toString() }
            else -> throw Exception("Unknown score type")
        }
    }

    override fun indexToScore(index: Int): Float {
        return when (scorePreference.getOrDefault()) {
            // 10 point
            POINT_10 -> index * 10f
            // 100 point
            POINT_100 -> index.toFloat()
            // 5 stars
            POINT_5 -> index * 20f
            // Smiley
            POINT_3 -> index * 30f
            // 10 point decimal
            POINT_10_DECIMAL -> index.toFloat()
            else -> throw Exception("Unknown score type")
        }
    }

    override fun displayScore(track: Track): String {
        val score = track.score

        return when (scorePreference.getOrDefault()) {
            POINT_5 -> "${(score / 20).toInt()} ★"
            POINT_3 -> when {
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
        // If user was using API v1 fetch library_id
        if (track.library_id == null || track.library_id!! == 0L){
            return api.findLibManga(track, getUsername().toInt()).flatMap {
                if (it == null) {
                    throw Exception("$track not found on user library")
                }
                track.library_id = it.library_id
                api.updateLibManga(track)
            }
        }

        return api.updateLibManga(track)
    }

    override fun bind(track: Track): Observable<Track> {
        return api.findLibManga(track, getUsername().toInt())
                .flatMap { remoteTrack ->
                    if (remoteTrack != null) {
                        track.copyPersonalFrom(remoteTrack)
                        track.library_id = remoteTrack.library_id
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
        return api.getLibManga(track, getUsername().toInt())
                .map { remoteTrack ->
                    track.copyPersonalFrom(remoteTrack)
                    track.total_chapters = remoteTrack.total_chapters
                    track
                }
    }

    override fun login(username: String, password: String) = login(password)

    fun login(token: String): Completable {
        val oauth = api.createOAuth(token)
        interceptor.setAuth(oauth)
        return api.getCurrentUser().map { (username, scoreType) ->
            scorePreference.set(scoreType)
            saveCredentials(username.toString(), oauth.access_token)
         }.doOnError{
            logout()
        }.toCompletable()
    }

    override fun logout() {
        super.logout()
        preferences.trackToken(this).set(null)
        interceptor.setAuth(null)
    }

    fun saveOAuth(oAuth: OAuth?) {
        preferences.trackToken(this).set(gson.toJson(oAuth))
    }

    fun loadOAuth(): OAuth? {
        return try {
            gson.fromJson(preferences.trackToken(this).get(), OAuth::class.java)
        } catch (e: Exception) {
            null
        }
    }

}

