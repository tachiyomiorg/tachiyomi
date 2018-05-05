package eu.kanade.tachiyomi.data.track.kitsu

import android.content.Context
import android.graphics.Color
import com.google.gson.Gson
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import rx.Completable
import rx.Observable
import uy.kohesive.injekt.injectLazy
import java.text.DecimalFormat

class Kitsu(private val context: Context, id: Int) : TrackService(id) {

    companion object {
        const val READING = 1
        const val COMPLETED = 2
        const val ON_HOLD = 3
        const val DROPPED = 4
        const val PLAN_TO_READ = 5

        const val DEFAULT_STATUS = READING
        const val DEFAULT_SCORE = 0f
    }

    override val name = "Kitsu"

    private val gson: Gson by injectLazy()

    private val interceptor by lazy { KitsuInterceptor(this, gson) }

    private val api by lazy { KitsuApi(client, interceptor) }

    override fun getLogo(): Int {
        return R.drawable.kitsu
    }

    override fun getLogoColor(): Int {
        return Color.rgb(51, 37, 50)
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

    override fun getScoreList(): List<String> {
        val df = DecimalFormat("0.#")
        return listOf("0") + IntRange(2, 20).map { df.format(it / 2f) }
    }

    override fun indexToScore(index: Int): Float {
        return if (index > 0) (index + 1) / 2f else 0f
    }

    override fun displayScore(track: Track): String {
        val df = DecimalFormat("0.#")
        return df.format(track.score)
    }

    override fun add(track: Track): Observable<Track> {
        return api.addLibManga(track, getUserId())
    }

    override fun update(track: Track): Observable<Track> {
        if (track.total_chapters != 0 && track.last_chapter_read == track.total_chapters) {
            track.status = COMPLETED
        }

        return api.updateLibManga(track)
    }

    override fun bind(track: Track): Observable<Track> {
        return api.findLibManga(track, getUserId())
                .flatMap { remoteTrack ->
                    if (remoteTrack != null) {
                        track.copyPersonalFrom(remoteTrack)
                        track.media_id = remoteTrack.media_id
                        update(track)
                    } else {
                        track.score = DEFAULT_SCORE
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

    override fun login(username: String, password: String): Completable {
        return api.login(username, password)
                .doOnNext { interceptor.newAuth(it) }
                .flatMap { api.getCurrentUser() }
                .doOnNext { userId -> saveCredentials(username, userId) }
                .doOnError { logout() }
                .toCompletable()
    }

    override fun logout() {
        super.logout()
        interceptor.newAuth(null)
    }

    private fun getUserId(): String {
        return getPassword()
    }

    fun saveToken(oauth: OAuth?) {
        val json = gson.toJson(oauth)
        preferences.trackToken(this).set(json)
    }

    fun restoreToken(): OAuth? {
        return try {
            gson.fromJson(preferences.trackToken(this).get(), OAuth::class.java)
        } catch (e: Exception) {
            null
        }
    }

}
