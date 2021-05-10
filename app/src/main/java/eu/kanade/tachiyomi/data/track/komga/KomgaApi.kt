package eu.kanade.tachiyomi.data.track.komga

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.network.parseAs
import eu.kanade.tachiyomi.util.lang.withIOContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber

class KomgaApi(private val client: OkHttpClient) {

    suspend fun getSeries(url: String): TrackSearch? {
        return withIOContext {
            try {
                client.newCall(
                    GET(url)
                )
                    .await()
                    .parseAs<SeriesDto>()
                    .toTrack()
                    .apply {
                        cover_url = "$url/thumbnail"
                        tracking_url = url
                    }
            } catch (e: Exception) {
                Timber.w(e, "Could not get Series: $url")
                null
            }
        }
    }

    suspend fun updateProgress(track: Track): Track {
        val progress = ReadProgressSeriesUpdateDto(track.last_chapter_read)
        val payload = Json.encodeToString(ReadProgressSeriesUpdateDto.serializer(), progress)
        client.newCall(
            Request.Builder()
                .url("${track.tracking_url}/read-progress/tachiyomi")
                .patch(payload.toRequestBody("application/json".toMediaType()))
                .build()
        )
            .await()
        return getSeries(track.tracking_url)!!
    }

    private fun SeriesDto.toTrack(): TrackSearch = TrackSearch.create(TrackManager.KOMGA).also {
        it.title = metadata.title
        it.total_chapters = booksCount
        it.summary = metadata.summary
        it.publishing_status = metadata.status
        it.status = when (booksCount) {
            booksUnreadCount -> Komga.UNREAD
            booksReadCount -> Komga.COMPLETED
            else -> Komga.READING
        }
        it.last_chapter_read = booksReadCount
    }
}
