@file:Suppress("UNCHECKED_CAST")

package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.FormBody
import okhttp3.RequestBody
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

private fun toMap(map: Any?) = map as? Map<String, Any?>

class YamlSourceNode(uncheckedMap: Map<*, *>) {

    val map = toMap(uncheckedMap)!!

    val id: Any by map

    val name: String by map

    val host: String by map

    val lang: String by map

    val client: String?
        get() = map["client"] as? String

    val popular = PopularNode(toMap(map["popular"])!!)

    val latestupdates = toMap(map["latest_updates"])?.let { LatestUpdatesNode(it) }

    val search = SearchNode(toMap(map["search"])!!)

    val manga = MangaNode(toMap(map["manga"])!!)

    val chapters = ChaptersNode(toMap(map["chapters"])!!)

    val pages = PagesNode(toMap(map["pages"])!!)
}

interface RequestableNode {

    val map: Map<String, Any?>

    val url: String
        get() = map["url"] as String

    val method: String?
        get() = map["method"] as? String

    val payload: Map<String, String>?
        get() = map["payload"] as? Map<String, String>

    fun createForm(): RequestBody {
        return FormBody.Builder().apply {
            payload?.let {
                for ((key, value) in it) {
                    add(key, value)
                }
            }
        }.build()
    }

}

class PopularNode(override val map: Map<String, Any?>): RequestableNode {

    val manga_css: String by map

    val next_url_css: String?
        get() = map["next_url_css"] as? String

}


class LatestUpdatesNode(override val map: Map<String, Any?>): RequestableNode {

    val manga_css: String by map

    val next_url_css: String?
        get() = map["next_url_css"] as? String

}


class SearchNode(override val map: Map<String, Any?>): RequestableNode {

    val manga_css: String by map

    val next_url_css: String?
        get() = map["next_url_css"] as? String
}

class MangaNode(private val map: Map<String, Any?>) {

    val parts = CacheNode(toMap(map["parts"]) ?: emptyMap())

    val artist = toMap(map["artist"])?.let { SelectableNode(it) }

    val author = toMap(map["author"])?.let { SelectableNode(it) }

    val summary = toMap(map["summary"])?.let { SelectableNode(it) }

    val status = toMap(map["status"])?.let { StatusNode(it) }

    val genres = toMap(map["genres"])?.let { SelectableNode(it) }

    val cover = toMap(map["cover"])?.let { CoverNode(it) }

}

class ChaptersNode(private val map: Map<String, Any?>) {

    val chapter_css: String by map

    val title: String by map

    val date = toMap(toMap(map["date"]))?.let { DateNode(it) }
}

class CacheNode(private val map: Map<String, Any?>) {

    fun get(document: Document) = map.mapValues { document.select(it.value as String).first() }
}

open class SelectableNode(private val map: Map<String, Any?>) {

    val select: String by map

    val from: String?
        get() = map["from"] as? String

    open val attr: String?
        get() = map["attr"] as? String

    val capture: String?
        get() = map["capture"] as? String

    fun process(document: Element, cache: Map<String, Element>): String {
        val parent = from?.let { cache[it] } ?: document
        val node = parent.select(select).first()
        var text = attr?.let { node.attr(it) } ?: node.text()
        capture?.let {
            text = Regex(it).find(text)?.groupValues?.get(1) ?: text
        }
        return text
    }
}

class StatusNode(private val map: Map<String, Any?>) : SelectableNode(map) {

    val complete: String?
        get() = map["complete"] as? String

    val ongoing: String?
        get() = map["ongoing"] as? String

    val licensed: String?
        get() = map["licensed"] as? String

    fun getStatus(document: Element, cache: Map<String, Element>): Int {
        val text = process(document, cache)
        complete?.let {
            if (text.contains(it)) return SManga.COMPLETED
        }
        ongoing?.let {
            if (text.contains(it)) return SManga.ONGOING
        }
        licensed?.let {
            if (text.contains(it)) return SManga.LICENSED
        }
        return SManga.UNKNOWN
    }
}

class CoverNode(private val map: Map<String, Any?>) : SelectableNode(map) {

    override val attr: String?
        get() = map["attr"] as? String ?: "src"
}

class DateNode(private val map: Map<String, Any?>) : SelectableNode(map) {

    val format: String by map

    fun getDate(document: Element, cache: Map<String, Element>, formatter: SimpleDateFormat): Date {
        val text = process(document, cache)
        try {
            return formatter.parse(text)
        } catch (exception: ParseException) {}

        for (i in 0..7) {
            (map["day$i"] as? List<String>)?.let {
                it.find { it.toRegex().containsMatchIn(text) }?.let {
                    return Calendar.getInstance().apply { add(Calendar.DATE, -i) }.time
                }
            }
        }

        return Date(0)
    }

}

class PagesNode(private val map: Map<String, Any?>) {

    val pages_regex: String?
        get() = map["pages_regex"] as? String

    val pages_css: String?
        get() = map["pages_css"] as? String

    val pages_attr: String?
        get() = map["pages_attr"] as? String ?: "value"

    val replace: String?
        get() = map["url_replace"] as? String

    val replacement: String?
        get() = map["url_replacement"] as? String

    val image_regex: String?
        get() = map["image_regex"] as? String

    val image_css: String?
        get() = map["image_css"] as? String

    val image_attr: String
        get() = map["image_attr"] as? String ?: "src"

}