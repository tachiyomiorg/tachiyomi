package eu.kanade.tachiyomi.data.track.komga

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.network.parseAs
import eu.kanade.tachiyomi.util.lang.withIOContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import uy.kohesive.injekt.injectLazy

const val READLIST_API = "/api/v1/readlists"

class KomgaApi(private val client: OkHttpClient) {

    private val json: Json by injectLazy()

    suspend fun getTrackSearch(url: String): TrackSearch =
        withIOContext {
            try {
                val track = if (url.contains(READLIST_API)) {
                    client.newCall(GET(url))
                        .await()
                        .parseAs<ReadListDto>()
                        .toTrack()
                } else {
                    client.newCall(GET(url))
                        .await()
                        .parseAs<SeriesDto>()
                        .toTrack()
                }

                val progress = client
                    .newCall(GET("$url/read-progress/tachiyomi"))
                    .await()
                    .parseAs<ReadProgressDto>()

                track.apply {
                    cover_url = "$url/thumbnail"
                    tracking_url = url
                    total_chapters = progress.booksCount
                    status = when (progress.booksCount) {
                        progress.booksUnreadCount -> Komga.UNREAD
                        progress.booksReadCount -> Komga.COMPLETED
                        else -> Komga.READING
                    }
                    last_chapter_read = progress.lastReadContinuousIndex
                }
            } catch (e: Exception) {
                Timber.w(e, "Could not get item: $url")
                throw e
            }
        }

    suspend fun updateProgress(track: Track): Track {
        val progress = ReadProgressUpdateDto(track.last_chapter_read)
        val payload = json.encodeToString(progress)
        client.newCall(
            Request.Builder()
                .url("${track.tracking_url}/read-progress/tachiyomi")
                .put(payload.toRequestBody("application/json".toMediaType()))
                .build()
        )
            .await()
        return getTrackSearch(track.tracking_url)
    }

    private fun SeriesDto.toTrack(): TrackSearch = TrackSearch.create(TrackManager.KOMGA).also {
        it.title = metadata.title
        it.summary = metadata.summary
        it.publishing_status = metadata.status
    }

    private fun ReadListDto.toTrack(): TrackSearch = TrackSearch.create(TrackManager.KOMGA).also {
        it.title = name
    }
}
