package eu.kanade.tachiyomi.data.backup.legacy.serializer

import com.github.salomonbrys.kotson.typeAdapter
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonToken
import eu.kanade.tachiyomi.data.database.models.ChapterImpl

/**
 * JSON Serializer used to write / read [ChapterImpl] to / from json
 */
object ChapterTypeAdapter {

    private const val URL = "u"
    private const val READ = "r"
    private const val BOOKMARK = "b"
    private const val LAST_READ = "l"

    fun build(): TypeAdapter<ChapterImpl> {
        return typeAdapter {
            write {
                if (it.read || it.bookmark || it.last_page_read != 0) {
                    beginObject()
                    name(URL)
                    value(it.url)
                    if (it.read) {
                        name(READ)
                        value(1)
                    }
                    if (it.bookmark) {
                        name(BOOKMARK)
                        value(1)
                    }
                    if (it.last_page_read != 0) {
                        name(LAST_READ)
                        value(it.last_page_read)
                    }
                    endObject()
                }
            }

            read {
                val chapter = ChapterImpl()
                beginObject()
                while (hasNext()) {
                    if (peek() == JsonToken.NAME) {
                        when (nextName()) {
                            URL -> chapter.url = nextString()
                            READ -> chapter.read = nextInt() == 1
                            BOOKMARK -> chapter.bookmark = nextInt() == 1
                            LAST_READ -> chapter.last_page_read = nextInt()
                        }
                    }
                }
                endObject()
                chapter
            }
        }
    }
}
