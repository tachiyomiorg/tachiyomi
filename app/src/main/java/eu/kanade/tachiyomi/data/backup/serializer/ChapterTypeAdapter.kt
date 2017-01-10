package eu.kanade.tachiyomi.data.backup.serializer

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
                        value(it.read)
                    }
                    if (it.bookmark) {
                        name(BOOKMARK)
                        value(it.bookmark)
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
                        val name = nextName()

                        when (name) {
                            URL -> chapter.url = nextString()
                            READ -> chapter.read = nextBoolean()
                            BOOKMARK -> chapter.bookmark = nextBoolean()
                            LAST_READ -> chapter.last_page_read = nextInt()
                            else -> {
                                if (peek() == JsonToken.NAME)
                                    nextName()
                                else if (peek() == JsonToken.BOOLEAN)
                                    nextBoolean()
                                else if (peek() == JsonToken.NUMBER)
                                    nextLong()
                                else if (peek() == JsonToken.STRING)
                                    nextString()
                            }
                        }
                    }
                }
                endObject()
                chapter
            }
        }
    }
}