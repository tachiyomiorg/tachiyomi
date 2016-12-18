package eu.kanade.tachiyomi.data.track.kitsu

import android.content.Context
import android.graphics.Color
import com.github.salomonbrys.kotson.*
import com.google.gson.Gson
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackService
import rx.Completable
import rx.Observable
import timber.log.Timber
import uy.kohesive.injekt.injectLazy

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

    private val api by lazy {
        KitsuApi.createService(client.newBuilder()
                .addInterceptor(interceptor)
                .build())
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

    override fun login(username: String, password: String): Completable {
        return KitsuApi.createLoginService(client)
                .requestAccessToken(username, password)
                .doOnNext { interceptor.newAuth(it) }
                .flatMap { api.getCurrentUser().map { it["data"].array[0]["id"].string } }
                .doOnNext { userId -> saveCredentials(username, userId) }
                .doOnError { logout() }
                .toCompletable()
    }

    override fun logout() {
        super.logout()
        interceptor.newAuth(null)
    }

    override fun search(query: String): Observable<List<Track>> {
        return api.search(query)
                .map { json ->
                    val data = json["data"].array
                    data.map { KitsuManga(it.obj).toTrack() }
                }
                .doOnError { Timber.e(it) }
    }

    override fun bind(track: Track): Observable<Track> {
        return find(track)
                .flatMap { remoteTrack ->
                    if (remoteTrack != null) {
                        track.copyPersonalFrom(remoteTrack)
                        track.remote_id = remoteTrack.remote_id
                        update(track)
                    } else {
                        track.score = DEFAULT_SCORE
                        track.status = DEFAULT_STATUS
                        add(track)
                    }
                }
    }

    private fun find(track: Track): Observable<Track?> {
        return api.findLibManga(getUserId(), track.remote_id)
                .map { json ->
                    val data = json["data"].array
                    if (data.size() > 0) {
                        KitsuLibManga(data[0].obj, json["included"].array[0].obj).toTrack()
                    } else {
                        null
                    }
                }
    }

    override fun add(track: Track): Observable<Track> {
        // @formatter:off
        val data = jsonObject(
            "type" to "libraryEntries",
            "attributes" to jsonObject(
                "status" to track.getKitsuStatus(),
                "progress" to track.last_chapter_read
            ),
            "relationships" to jsonObject(
                "user" to jsonObject(
                    "data" to jsonObject(
                        "id" to getUserId(),
                        "type" to "users"
                    )
                ),
                "media" to jsonObject(
                    "data" to jsonObject(
                        "id" to track.remote_id,
                        "type" to "manga"
                    )
                )
            )
        )
        // @formatter:on

        return api.addLibManga(jsonObject("data" to data))
                .doOnNext { json -> track.remote_id = json["data"]["id"].int }
                .doOnError { Timber.e(it) }
                .map { track }
    }

    override fun update(track: Track): Observable<Track> {
        if (track.total_chapters != 0 && track.last_chapter_read == track.total_chapters) {
            track.status = COMPLETED
        }
        // @formatter:off
        val data = jsonObject(
            "type" to "libraryEntries",
            "id" to track.remote_id,
            "attributes" to jsonObject(
                "status" to track.getKitsuStatus(),
                "progress" to track.last_chapter_read,
                "rating" to track.getKitsuScore()
            )
        )
        // @formatter:on

        return api.updateLibManga(track.remote_id, jsonObject("data" to data))
                .map { track }
    }

    override fun refresh(track: Track): Observable<Track> {
        return api.getLibManga(track.remote_id)
                .map { json ->
                    val data = json["data"].array
                    if (data.size() > 0) {
                        val include = json["included"].array[0].obj
                        val remoteTrack = KitsuLibManga(data[0].obj, include).toTrack()
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

    private fun Track.getKitsuStatus() = when (status) {
        READING -> "current"
        COMPLETED -> "completed"
        ON_HOLD -> "on_hold"
        DROPPED -> "dropped"
        PLAN_TO_READ -> "planned"
        else -> throw Exception("Unknown status")
    }

    private fun Track.getKitsuScore(): String {
        return if (score > 0) (score / 2).toString() else ""
    }

    override fun getLogo(): Int {
        return R.drawable.kitsu
    }

    override fun getLogoColor(): Int {
        return Color.rgb(51, 37, 50)
    }

    override fun maxScore(): Int {
        return 10
    }

    override fun formatScore(track: Track): String {
        return track.getKitsuScore()
    }

}