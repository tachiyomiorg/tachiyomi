package eu.kanade.tachiyomi.data.backup.legacy.serializer

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.ChapterImpl
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * JSON Serializer used to write / read [ChapterImpl] to / from json
 */
open class ChapterBaseSerializer<T : Chapter> : KSerializer<T> {

    override val descriptor = buildClassSerialDescriptor("Chapter")

    override fun serialize(encoder: Encoder, value: T) {
        encoder as JsonEncoder
        encoder.encodeJsonElement(
            buildJsonObject {
                put(URL, value.url)
                if (value.read) {
                    put(READ, 1)
                }
                if (value.bookmark) {
                    put(BOOKMARK, 1)
                }
                if (value.last_page_read != 0) {
                    put(LAST_READ, value.last_page_read)
                }
            }
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(decoder: Decoder): T {
        // make a chapter impl and cast as T so that the serializer accepts it
        return ChapterImpl().apply {
            decoder as JsonDecoder
            val jsonObject = decoder.decodeJsonElement().jsonObject
            url = jsonObject[URL]!!.jsonPrimitive.content
            read = jsonObject[READ]?.jsonPrimitive?.intOrNull == 1
            bookmark = jsonObject[BOOKMARK]?.jsonPrimitive?.intOrNull == 1
            last_page_read = jsonObject[LAST_READ]?.jsonPrimitive?.intOrNull ?: last_page_read
        } as T
    }

    companion object {
        private const val URL = "u"
        private const val READ = "r"
        private const val BOOKMARK = "b"
        private const val LAST_READ = "l"
    }
}

// Allow for serialization of a chapter and chapter impl
object ChapterTypeSerializer : ChapterBaseSerializer<Chapter>()

object ChapterImplTypeSerializer : ChapterBaseSerializer<ChapterImpl>()
