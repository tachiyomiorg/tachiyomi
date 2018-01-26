package eu.kanade.tachiyomi.data.track.myanimelist

import android.content.Context
import android.graphics.Color
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.data.track.TrackService
import rx.Completable
import rx.Observable

class Myanimelist(private val context: Context, id: Int) : TrackService(id) {

    companion object {

        const val READING = 1
        const val COMPLETED = 2
        const val ON_HOLD = 3
        const val DROPPED = 4
        const val PLAN_TO_READ = 6

        const val DEFAULT_STATUS = READING
        const val DEFAULT_SCORE = 0
    }

    private val api by lazy { MyanimelistApi(client, getUsername(), getPassword()) }

    override val name: String
        get() = "MyAnimeList"

    override fun getLogo() = R.drawable.mal

    override fun getLogoColor() = Color.rgb(46, 81, 162)

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

    override fun getStatusList(): List<Int> {
        return listOf(READING, COMPLETED, ON_HOLD, DROPPED, PLAN_TO_READ)
    }

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
        return api.search(query, getUsername())
    }

    override fun refresh(track: Track): Observable<Track> {
        return api.getLibManga(track, getUsername())
                .map { remoteTrack ->
                    track.copyPersonalFrom(remoteTrack)
                    track.total_chapters = remoteTrack.total_chapters
                    track
                }
    }

    override fun login(username: String, password: String): Completable {
        return api.login(username, password)
                .doOnNext { saveCredentials(username, password) }
                .doOnError { logout() }
                .toCompletable()
    }

}
