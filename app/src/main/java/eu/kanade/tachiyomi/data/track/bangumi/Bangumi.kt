package eu.kanade.tachiyomi.data.track.bangumi

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

class Bangumi(private val context: Context, id: Int) : TrackService(id) {

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
    return api.statusLibManga(track)
      .flatMap {
        api.findLibManga(track).flatMap { remoteTrack ->
          if (remoteTrack != null && it != null) {
            track.copyPersonalFrom(remoteTrack)
            track.library_id = remoteTrack.library_id
            track.status = remoteTrack.status
            track.last_chapter_read = remoteTrack.last_chapter_read
            update(track)
          } else {
            // Set default fields if it's not found in the list
            track.score = DEFAULT_SCORE.toFloat()
            track.status = DEFAULT_STATUS
            add(track)
            update(track)
          }
        }
      }
  }

  override fun search(query: String): Observable<List<TrackSearch>> {
    return api.search(query)
  }

  override fun refresh(track: Track): Observable<Track> {
    return api.statusLibManga(track)
      .flatMap {
        track.copyPersonalFrom(it!!)
        api.findLibManga(track)
          .map { remoteTrack ->
            if (remoteTrack != null) {
              track.total_chapters = remoteTrack.total_chapters
              track.status = remoteTrack.status
            }
            track
          }
      }
  }

  companion object {
    const val READING = 3
    const val COMPLETED = 2
    const val ON_HOLD = 4
    const val DROPPED = 5
    const val PLANNING = 1

    const val DEFAULT_STATUS = READING
    const val DEFAULT_SCORE = 0
  }

  override val name = "Bangumi"

  private val gson: Gson by injectLazy()

  private val interceptor by lazy { BangumiInterceptor(this, gson) }

  private val api by lazy { BangumiApi(client, interceptor) }

  override fun getLogo() = R.drawable.bangumi

  override fun getLogoColor() = Color.rgb(0xF0, 0x91, 0x99)

  override fun getStatusList(): List<Int> {
    return listOf(READING, COMPLETED, ON_HOLD, DROPPED, PLANNING)
  }

  override fun getStatus(status: Int): String = with(context) {
    when (status) {
      READING -> getString(R.string.reading)
      COMPLETED -> getString(R.string.completed)
      ON_HOLD -> getString(R.string.on_hold)
      DROPPED -> getString(R.string.dropped)
      PLANNING -> getString(R.string.plan_to_read)
      else -> ""
    }
  }

  override fun login(username: String, password: String) = login(password)

  fun login(code: String): Completable {
    return api.accessToken(code).map { oauth: OAuth? ->
      interceptor.newAuth(oauth)
      if (oauth != null) {
        saveCredentials(oauth.user_id.toString(), oauth.access_token)
      }
    }.doOnError {
      logout()
    }.toCompletable()
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

  override fun logout() {
    super.logout()
    preferences.trackToken(this).set(null)
    interceptor.newAuth(null)
  }
}
