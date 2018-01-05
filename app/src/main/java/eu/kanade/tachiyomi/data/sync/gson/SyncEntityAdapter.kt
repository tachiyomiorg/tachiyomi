package eu.kanade.tachiyomi.data.sync.gson

import com.github.salomonbrys.kotson.obj
import com.github.salomonbrys.kotson.string
import com.google.gson.*
import eu.kanade.tachiyomi.data.sync.protocol.models.common.SyncEntity
import eu.kanade.tachiyomi.data.sync.protocol.models.entities.SyncManga
import java.lang.reflect.Type

/**
 * Serializer for sync entities. Allows distinguishing between types of sync entities during
 * serialization/deserialization.
 */
class SyncEntityAdapter : JsonSerializer<SyncEntity<*>>, JsonDeserializer<SyncEntity<*>> {
    //Use SyncManga class to get sync entity package
    private val syncEntityPackage = SyncManga::class.java.`package`.name
    
    override fun serialize(src: SyncEntity<*>, typeOfSrc: Type,
                           context: JsonSerializationContext): JsonElement {
        val retValue = JsonObject()
        retValue.addProperty(CLASSNAME, serializeClass(src::class.java))
        val elem = context.serialize(src)
        retValue.add(INSTANCE, elem)
        return retValue
    }

    override fun deserialize(json: JsonElement, typeOfT: Type,
                    context: JsonDeserializationContext): SyncEntity<*> {
        val obj = json.obj
        val className = obj[CLASSNAME].string

        val klass: Class<*>?
        try {
            klass = deserializeClass(className)
        } catch (e: ClassNotFoundException) {
            throw JsonParseException(e)
        }

        return context.deserialize<SyncEntity<*>>(obj[INSTANCE], klass)
    }
    
    private fun serializeClass(clazz: Class<*>)
        = clazz.simpleName.removePrefix("Sync")
    
    private fun deserializeClass(string: String): Class<*>
        = Class.forName("$syncEntityPackage.Sync$string")

    companion object {
        private val CLASSNAME = "t"
        private val INSTANCE = "v"
    }
}