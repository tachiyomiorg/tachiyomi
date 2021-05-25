package eu.kanade.tachiyomi.data.track.kitsu

import android.content.Context
import android.graphics.Color
import androidx.annotation.StringRes
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import uy.kohesive.injekt.injectLazy
import java.text.DecimalFormat

class Kitsu(private val context: Context, id: Int) : TrackService(id) {

    companion object {
        const val READING = 1
        const val COMPLETED = 2
        const val ON_HOLD = 3
        const val DROPPED = 4
        const val PLAN_TO_READ = 5
    }

    @StringRes
    override fun nameRes() = R.string.tracker_kitsu

    private val json: Json by injectLazy()

    private val interceptor by lazy { KitsuInterceptor(this) }

    private val api by lazy { KitsuApi(client, interceptor) }

    override fun getLogo() = R.drawable.ic_tracker_kitsu

    override fun getLogoColor() = Color.rgb(51, 37, 50)

    override fun getStatusList(): List<Int> {
        return listOf(READING, PLAN_TO_READ, COMPLETED, ON_HOLD, DROPPED)
    }

    override fun getStatus(status: Int): String = with(context) {
        when (status) {
            READING -> getString(R.string.currently_reading)
            PLAN_TO_READ -> getString(R.string.want_to_read)
            COMPLETED -> getString(R.string.completed)
            ON_HOLD -> getString(R.string.on_hold)
            DROPPED -> getString(R.string.dropped)
            else -> ""
        }
    }

    override fun getCompletionStatus(): Int = COMPLETED

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

    private suspend fun add(track: Track): Track {
        return api.addLibManga(track, getUserId())
    }

    override suspend fun update(track: Track): Track {
        return api.updateLibManga(track)
    }

    override suspend fun bind(track: Track): Track {
        val remoteTrack = api.findLibManga(track, getUserId())
        return if (remoteTrack != null) {
            track.copyPersonalFrom(remoteTrack)
            track.media_id = remoteTrack.media_id
            update(track)
        } else {
            track.status = READING
            track.score = 0F
            add(track)
        }
    }

    override suspend fun search(query: String): List<TrackSearch> {
        return api.search(query)
    }

    override suspend fun refresh(track: Track): Track {
        val remoteTrack = api.getLibManga(track)
        track.copyPersonalFrom(remoteTrack)
        track.total_chapters = remoteTrack.total_chapters
        return track
    }

    override suspend fun login(username: String, password: String) {
        val token = api.login(username, password)
        interceptor.newAuth(token)
        val userId = api.getCurrentUser()
        saveCredentials(username, userId)
    }

    override fun logout() {
        super.logout()
        interceptor.newAuth(null)
    }

    private fun getUserId(): String {
        return getPassword()
    }

    fun saveToken(oauth: OAuth?) {
        preferences.trackToken(this).set(json.encodeToString(oauth))
    }

    fun restoreToken(): OAuth? {
        return try {
            json.decodeFromString<OAuth>(preferences.trackToken(this).get())
        } catch (e: Exception) {
            null
        }
    }
}
