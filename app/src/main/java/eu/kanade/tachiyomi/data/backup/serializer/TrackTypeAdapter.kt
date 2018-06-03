package eu.kanade.tachiyomi.data.backup.serializer

import android.telecom.DisconnectCause.REMOTE
import com.github.salomonbrys.kotson.typeAdapter
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonToken
import eu.kanade.tachiyomi.data.database.models.TrackImpl

/**
 * JSON Serializer used to write / read [TrackImpl] to / from json
 */
object TrackTypeAdapter {

    private const val SYNC = "s"
    private const val MEDIA = "r"
    private const val LIBRARY = "ml"
    private const val TITLE = "t"
    private const val LAST_READ = "l"
    private const val TRACKING_URL = "u"

    fun build(): TypeAdapter<TrackImpl> {
        return typeAdapter {
            write {
                beginObject()
                name(TITLE)
                value(it.title)
                name(SYNC)
                value(it.sync_id)
                name(MEDIA)
                value(it.media_id)
                name(LIBRARY)
                value(it.library_id)
                name(LAST_READ)
                value(it.last_chapter_read)
                name(TRACKING_URL)
                value(it.tracking_url)
                endObject()
            }

            read {
                val track = TrackImpl()
                beginObject()
                while (hasNext()) {
                    if (peek() == JsonToken.NAME) {
                        val name = nextName()

                        when (name) {
                            TITLE -> track.title = nextString()
                            SYNC -> track.sync_id = nextInt()
                            MEDIA -> track.media_id = nextInt()
                            LIBRARY -> track.library_id = nextLong()
                            LAST_READ -> track.last_chapter_read = nextInt()
                            TRACKING_URL -> track.tracking_url = nextString()
                        }
                    }
                }
                endObject()
                track
            }
        }
    }
}