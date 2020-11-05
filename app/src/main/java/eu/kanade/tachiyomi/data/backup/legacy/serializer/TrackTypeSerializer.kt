package eu.kanade.tachiyomi.data.backup.legacy.serializer

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.database.models.TrackImpl
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put

/**
 * JSON Serializer used to write / read [TrackImpl] to / from json
 */
open class TrackBaseSerializer<T : Track> : KSerializer<T> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Track")

    override fun serialize(encoder: Encoder, value: T) {
        encoder as JsonEncoder
        encoder.encodeJsonElement(
            buildJsonObject {
                put(TITLE, value.title)
                put(SYNC, value.sync_id)
                put(MEDIA, value.media_id)
                put(LIBRARY, value.library_id)
                put(LAST_READ, value.last_chapter_read)
                put(TRACKING_URL, value.tracking_url)
            }
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(decoder: Decoder): T {
        // make a track impl and cast as T so that the serializer accepts it
        return TrackImpl().apply {
            decoder as JsonDecoder
            val jsonObject = decoder.decodeJsonElement().jsonObject
            title = jsonObject[TITLE]!!.jsonPrimitive.content
            sync_id = jsonObject[SYNC]!!.jsonPrimitive.int
            media_id = jsonObject[MEDIA]!!.jsonPrimitive.int
            library_id = jsonObject[LIBRARY]!!.jsonPrimitive.long
            last_chapter_read = jsonObject[LAST_READ]!!.jsonPrimitive.int
            tracking_url = jsonObject[TRACKING_URL]!!.jsonPrimitive.content
        } as T
    }

    companion object {
        private const val SYNC = "s"
        private const val MEDIA = "r"
        private const val LIBRARY = "ml"
        private const val TITLE = "t"
        private const val LAST_READ = "l"
        private const val TRACKING_URL = "u"
    }
}

// Allow for serialization of a track and track impl
object TrackTypeSerializer : TrackBaseSerializer<Track>()

object TrackImplTypeSerializer : TrackBaseSerializer<TrackImpl>()
