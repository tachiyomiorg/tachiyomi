package eu.kanade.tachiyomi.data.track.myanimelist

import android.content.Context
import android.graphics.Color
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.util.lang.runAsObservable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rx.Observable
import uy.kohesive.injekt.injectLazy

class MyAnimeList(private val context: Context, id: Int) : TrackService(id) {

    companion object {
        const val READING = 1
        const val COMPLETED = 2
        const val ON_HOLD = 3
        const val DROPPED = 4
        const val PLAN_TO_READ = 6
        const val REREADING = 7
    }

    private val json: Json by injectLazy()

    private val interceptor by lazy { MyAnimeListInterceptor(this, getPassword()) }
    private val api by lazy { MyAnimeListApi(client, interceptor) }

    override val name: String
        get() = "MyAnimeList"

    override fun getLogo() = R.drawable.ic_tracker_mal

    override fun getLogoColor() = Color.rgb(46, 81, 162)

    override fun getStatusList(): List<Int> {
        return listOf(READING, COMPLETED, ON_HOLD, DROPPED, PLAN_TO_READ, REREADING)
    }

    override fun getStatus(status: Int): String = with(context) {
        when (status) {
            READING -> getString(R.string.reading)
            COMPLETED -> getString(R.string.completed)
            ON_HOLD -> getString(R.string.on_hold)
            DROPPED -> getString(R.string.dropped)
            PLAN_TO_READ -> getString(R.string.plan_to_read)
            REREADING -> getString(R.string.repeating)
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

    override suspend fun add(track: Track): Track {
        return api.addItemToList(track)
    }

    override suspend fun update(track: Track): Track {
        return api.updateItem(track)
    }

    override fun bind(track: Track): Observable<Track> {
        return runAsObservable({
            val remoteTrack = api.findListItem(track)
            if (remoteTrack != null) {
                track.copyPersonalFrom(remoteTrack)
                track.media_id = remoteTrack.media_id
                update(track)
            } else {
                add(track)
            }
        })
    }

    override fun search(query: String): Observable<List<TrackSearch>> {
        if (query.startsWith("my:")) {
            query.substringAfter("my:").toIntOrNull()?.let { id ->
                return runAsObservable({ listOf(api.getMangaDetails(id)) })
            }
        }

        return runAsObservable({ api.search(query) })
    }

    override fun refresh(track: Track): Observable<Track> {
        return runAsObservable({ api.getListItem(track) })
    }

    override suspend fun login(username: String, password: String) = login(password)

    suspend fun login(authCode: String) {
        try {
            val oauth = api.getAccessToken(authCode)
            interceptor.setAuth(oauth)
            val username = api.getCurrentUser()
            saveCredentials(username, oauth.access_token)
        } catch (e: Throwable) {
            logout()
        }
    }

    override fun logout() {
        super.logout()
        preferences.trackToken(this).delete()
        interceptor.setAuth(null)
    }

    fun saveOAuth(oAuth: OAuth?) {
        preferences.trackToken(this).set(json.encodeToString(oAuth))
    }

    fun loadOAuth(): OAuth? {
        return try {
            json.decodeFromString<OAuth>(preferences.trackToken(this).get())
        } catch (e: Exception) {
            null
        }
    }
}
