package eu.kanade.tachiyomi.data.track.shikimori

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

class Shikimori(private val context: Context, id: Int) : TrackService(id) {

    companion object {
        const val READING = 1
        const val COMPLETED = 2
        const val ON_HOLD = 3
        const val DROPPED = 4
        const val PLANNING = 5
        const val REPEATING = 6
    }

    private val json: Json by injectLazy()

    private val interceptor by lazy { ShikimoriInterceptor(this) }

    private val api by lazy { ShikimoriApi(client, interceptor) }

    @StringRes
    override fun nameRes() = R.string.tracker_shikimori

    override fun getScoreList(): List<String> {
        return IntRange(0, 10).map(Int::toString)
    }

    override fun displayScore(track: Track): String {
        return track.score.toInt().toString()
    }

    private suspend fun add(track: Track): Track {
        return api.addLibManga(track, getUsername())
    }

    override suspend fun update(track: Track, didReadChapter: Boolean): Track {
        if (track.status != COMPLETED) {
            if (track.status != REPEATING && didReadChapter) {
                track.status = READING
            }
        }

        return api.updateLibManga(track, getUsername())
    }

    override suspend fun bind(track: Track, hasReadChapters: Boolean): Track {
        val remoteTrack = api.findLibManga(track, getUsername())
        return if (remoteTrack != null) {
            track.copyPersonalFrom(remoteTrack)
            track.library_id = remoteTrack.library_id

            if (track.status != COMPLETED) {
                val isRereading = track.status == REPEATING
                track.status = if (isRereading.not() && hasReadChapters) READING else track.status
            }

            update(track)
        } else {
            // Set default fields if it's not found in the list
            track.status = if (hasReadChapters) READING else PLANNING
            track.score = 0F
            add(track)
        }
    }

    override suspend fun search(query: String): List<TrackSearch> {
        return api.search(query)
    }

    override suspend fun refresh(track: Track): Track {
        api.findLibManga(track, getUsername())?.let { remoteTrack ->
            track.copyPersonalFrom(remoteTrack)
            track.total_chapters = remoteTrack.total_chapters
        }
        return track
    }

    override fun getLogo() = R.drawable.ic_tracker_shikimori

    override fun getLogoColor() = Color.rgb(40, 40, 40)

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

    override fun getReadingStatus(): Int = READING

    override fun getRereadingStatus(): Int = REPEATING

    override fun getCompletionStatus(): Int = COMPLETED

    override suspend fun login(username: String, password: String) = login(password)

    suspend fun login(code: String) {
        try {
            val oauth = api.accessToken(code)
            interceptor.newAuth(oauth)
            val user = api.getCurrentUser()
            saveCredentials(user.toString(), oauth.access_token)
        } catch (e: Throwable) {
            logout()
        }
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

    override fun logout() {
        super.logout()
        preferences.trackToken(this).delete()
        interceptor.newAuth(null)
    }
}
