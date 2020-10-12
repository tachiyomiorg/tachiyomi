package eu.kanade.tachiyomi.data.backup.legacy.serializer

import com.github.salomonbrys.kotson.typeAdapter
import com.google.gson.TypeAdapter
import eu.kanade.tachiyomi.data.backup.legacy.models.DHistory

/**
 * JSON Serializer used to write / read [DHistory] to / from json
 */
object HistoryTypeAdapter {

    fun build(): TypeAdapter<DHistory> {
        return typeAdapter {
            write {
                if (it.lastRead != 0L) {
                    beginArray()
                    value(it.url)
                    value(it.lastRead)
                    endArray()
                }
            }

            read {
                beginArray()
                val url = nextString()
                val lastRead = nextLong()
                endArray()
                DHistory(url, lastRead)
            }
        }
    }
}
