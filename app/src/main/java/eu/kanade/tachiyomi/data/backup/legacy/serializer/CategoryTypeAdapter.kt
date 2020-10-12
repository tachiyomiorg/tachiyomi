package eu.kanade.tachiyomi.data.backup.legacy.serializer

import com.github.salomonbrys.kotson.typeAdapter
import com.google.gson.TypeAdapter
import eu.kanade.tachiyomi.data.database.models.CategoryImpl

/**
 * JSON Serializer used to write / read [CategoryImpl] to / from json
 */
object CategoryTypeAdapter {

    fun build(): TypeAdapter<CategoryImpl> {
        return typeAdapter {
            write {
                beginArray()
                value(it.name)
                value(it.order)
                endArray()
            }

            read {
                beginArray()
                val category = CategoryImpl()
                category.name = nextString()
                category.order = nextInt()
                endArray()
                category
            }
        }
    }
}
