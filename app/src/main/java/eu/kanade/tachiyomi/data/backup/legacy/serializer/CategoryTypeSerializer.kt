package eu.kanade.tachiyomi.data.backup.legacy.serializer

import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.CategoryImpl
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

/**
 * JSON Serializer used to write / read [CategoryImpl] to / from json
 */
open class CategoryBaseSerializer<T : Category> : KSerializer<T> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Category")

    override fun serialize(encoder: Encoder, value: T) {
        encoder as JsonEncoder
        encoder.encodeJsonElement(
            buildJsonArray {
                add(value.name)
                add(value.order)
            }
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(decoder: Decoder): T {
        // make a category impl and cast as T so that the serializer accepts it
        return CategoryImpl().apply {
            decoder as JsonDecoder
            val array = decoder.decodeJsonElement().jsonArray
            name = array[0].jsonPrimitive.content
            order = array[1].jsonPrimitive.int
        } as T
    }
}

// Allow for serialization of a category and category impl
object CategoryTypeSerializer : CategoryBaseSerializer<Category>()

object CategoryImplTypeSerializer : CategoryBaseSerializer<CategoryImpl>()
