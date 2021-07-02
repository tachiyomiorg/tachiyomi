package eu.kanade.tachiyomi.data.backup.legacy.serializer

import eu.kanade.tachiyomi.data.backup.legacy.models.DHistory
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

/**
 * JSON Serializer used to write / read [DHistory] to / from json
 */
object HistoryTypeSerializer : KSerializer<DHistory> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("History")

    override fun serialize(encoder: Encoder, value: DHistory) {
        encoder as JsonEncoder
        encoder.encodeJsonElement(
            buildJsonArray {
                add(value.url)
                add(value.lastRead)
            }
        )
    }

    override fun deserialize(decoder: Decoder): DHistory {
        decoder as JsonDecoder
        val array = decoder.decodeJsonElement().jsonArray
        return DHistory(
            url = array[0].jsonPrimitive.content,
            lastRead = array[1].jsonPrimitive.long
        )
    }
}
