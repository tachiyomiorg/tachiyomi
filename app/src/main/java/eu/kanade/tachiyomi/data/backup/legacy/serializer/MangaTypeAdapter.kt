package eu.kanade.tachiyomi.data.backup.legacy.serializer

import com.github.salomonbrys.kotson.typeAdapter
import com.google.gson.TypeAdapter
import eu.kanade.tachiyomi.data.database.models.MangaImpl

/**
 * JSON Serializer used to write / read [MangaImpl] to / from json
 */
object MangaTypeAdapter {

    fun build(): TypeAdapter<MangaImpl> {
        return typeAdapter {
            write {
                beginArray()
                value(it.url)
                value(it.title)
                value(it.source)
                value(it.viewer_flags)
                value(it.chapter_flags)
                endArray()
            }

            read {
                beginArray()
                val manga = MangaImpl()
                manga.url = nextString()
                manga.title = nextString()
                manga.source = nextLong()
                manga.viewer_flags = nextInt()
                manga.chapter_flags = nextInt()
                endArray()
                manga
            }
        }
    }
}
