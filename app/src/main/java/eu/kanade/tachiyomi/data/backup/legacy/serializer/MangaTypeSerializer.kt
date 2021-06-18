package eu.kanade.tachiyomi.data.backup.legacy.serializer

import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

/**
 * JSON Serializer used to write / read [MangaImpl] to / from json
 */
open class MangaBaseSerializer<T : Manga> : KSerializer<T> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Manga")

    override fun serialize(encoder: Encoder, value: T) {
        encoder as JsonEncoder
        encoder.encodeJsonElement(
            buildJsonArray {
                add(value.url)
                add(value.title)
                add(value.source)
                add(value.viewer_flags)
                add(value.chapter_flags)
            }
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(decoder: Decoder): T {
        // make a manga impl and cast as T so that the serializer accepts it
        return MangaImpl().apply {
            decoder as JsonDecoder
            val array = decoder.decodeJsonElement().jsonArray
            url = array[0].jsonPrimitive.content
            title = array[1].jsonPrimitive.content
            source = array[2].jsonPrimitive.long
            viewer_flags = array[3].jsonPrimitive.int
            chapter_flags = array[4].jsonPrimitive.int
        } as T
    }
}

// Allow for serialization of a manga and manga impl
object MangaTypeSerializer : MangaBaseSerializer<Manga>()

object MangaImplTypeSerializer : MangaBaseSerializer<MangaImpl>()
