package eu.kanade.tachiyomi.data.sync.gson

import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonObject
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonSerializer
import eu.kanade.tachiyomi.data.sync.protocol.models.common.SyncEntity
import java.lang.reflect.Type

class SyncEntityAdapter : JsonSerializer<SyncEntity<*>>, JsonDeserializer<SyncEntity<*>> {
    override fun serialize(src: SyncEntity<*>, typeOfSrc: Type,
                           context: JsonSerializationContext): JsonElement {
        val retValue = JsonObject()
        val className = src::class.java.name
        retValue.addProperty(CLASSNAME, className)
        val elem = context.serialize(src)
        retValue.add(INSTANCE, elem)
        return retValue
    }

    override fun deserialize(json: JsonElement, typeOfT: Type,
                    context: JsonDeserializationContext): SyncEntity<*> {
        val jsonObject = json.asJsonObject
        val prim = jsonObject.get(CLASSNAME) as JsonPrimitive
        val className = prim.asString

        val klass: Class<*>?
        try {
            klass = Class.forName(className)
        } catch (e: ClassNotFoundException) {
            throw JsonParseException(e)
        }

        return context.deserialize<SyncEntity<*>>(jsonObject.get(INSTANCE), klass)
    }

    companion object {

        private val CLASSNAME = "type"
        private val INSTANCE = "value"
    }
}