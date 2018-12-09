package tachiyomi.ext.all.mangadex

import com.github.salomonbrys.kotson.forEach
import com.github.salomonbrys.kotson.int
import com.github.salomonbrys.kotson.long
import com.github.salomonbrys.kotson.nullString
import com.github.salomonbrys.kotson.string
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import org.jsoup.Jsoup
import tachiyomi.core.http.GET
import tachiyomi.source.DeepLink
import tachiyomi.source.DeepLinkSource
import tachiyomi.source.Dependencies
import tachiyomi.source.HttpSource
import tachiyomi.source.model.ChapterInfo
import tachiyomi.source.model.Filter
import tachiyomi.source.model.FilterList
import tachiyomi.source.model.Listing
import tachiyomi.source.model.MangaInfo
import tachiyomi.source.model.MangasPageInfo
import tachiyomi.source.model.PageInfo
import tachiyomi.source.util.asJsoup
import java.net.URLEncoder
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.collections.set

open class Mangadex(
  private val deps: Dependencies
) : HttpSource(deps), DeepLinkSource {

  override val name = "MangaDex"

  override val baseUrl = "https://mangadex.org"

  override val client = clientBuilder(NO_R18)

  override val lang: String
    get() = "en"

  private val langCode get() = 1

  private fun clientBuilder(r18Toggle: Int): OkHttpClient = deps.http.cloudflareClient.newBuilder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .addNetworkInterceptor { chain ->
      val newReq = chain
        .request()
        .newBuilder()
        .addHeader("Cookie", cookiesHeader(r18Toggle, langCode))
        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64)")
        .build()
      chain.proceed(newReq)
    }.build()!!

  private fun cookiesHeader(r18Toggle: Int, langCode: Int): String {
    val cookies = mutableMapOf<String, String>()
    cookies["mangadex_h_toggle"] = r18Toggle.toString()
    cookies["mangadex_filter_langs"] = langCode.toString()
    return buildCookies(cookies)
  }

  private fun buildCookies(cookies: Map<String, String>) =
    cookies.entries.joinToString(separator = "; ", postfix = ";") {
      "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}"
    }

  override fun getRegex(): Regex {
    return Regex("""(?<=Ch\.) *([0-9]+)(\.[0-9]+)?""")
  }

  override fun fetchMangaList(listing: Listing?, page: Int): MangasPageInfo {
    val request = GET("$baseUrl/titles/0/$page", headers)
    val document = client.newCall(request).execute().asJsoup()

    val mangas = document.select("div.col-lg-6.border-bottom.pl-0.my-1").map { element ->
      val titleElement = element.select("a.manga_title").first()
      val coverElement = element.select("div.large_logo img").first()

      MangaInfo(
        key = removeMangaNameFromUrl(titleElement.attr("href")),
        title = titleElement.text().trim(),
        cover = baseUrl + coverElement.attr("src")
      )
    }

    val hasNextPage = document
      .select(".pagination li:not(.disabled) span[title*=last page]:not(disabled)")
      .first() != null

    return MangasPageInfo(mangas, hasNextPage)
  }

  override fun fetchMangaList(filters: FilterList, page: Int): MangasPageInfo {
    return fetchMangaList(null, 1)
  }

  override fun fetchMangaDetails(manga: MangaInfo): MangaInfo {
    val request = GET(baseUrl + API_MANGA + getMangaId(manga.key), headers)
    val response = client.newCall(request).execute()

    val jsonData = response.body()!!.string()
    val json = JsonParser().parse(jsonData).asJsonObject
    val mangaJson = json.getAsJsonObject("manga")
    val title = mangaJson.get("title").string
    val cover = baseUrl + mangaJson.get("cover_url").string
    val description = cleanString(mangaJson.get("description").string)
    val author = mangaJson.get("author").string
    val artist = mangaJson.get("artist").string
    val status = parseStatus(mangaJson.get("status").int)
    val genres = mutableListOf<String>()

//    mangaJson.get("genres").asJsonArray.forEach { id ->
//      getGenreList().find { it -> it.id == id.string }?.let { genre ->
//        genres.add(genre.name)
//      }
//    }
    val genre = genres.joinToString(", ")

    return MangaInfo(
      key = "",
      title = title,
      artist = artist,
      author = author,
      description = description,
      genres = genre,
      status = status,
      cover = cover,
      initialized = true
    )
  }

  override fun fetchChapterList(manga: MangaInfo): List<ChapterInfo> {
    val request = GET(baseUrl + API_MANGA + getMangaId(manga.key), headers)
    val response = client.newCall(request).execute()

    val now = Date().time
    var jsonData = response.body()!!.string()
    val json = JsonParser().parse(jsonData).asJsonObject
    val chapterJson = json.getAsJsonObject("chapter")
    val chapters = mutableListOf<ChapterInfo>()

    //skip chapters that dont match the desired language, or are future releases
    chapterJson?.forEach { key, jsonElement ->
      val chapterElement = jsonElement.asJsonObject
      if (chapterElement.get("lang_code").string == "gb" &&
        (chapterElement.get("timestamp").asLong * 1000) <= now) {

        chapterElement.toString()
        chapters.add(chapterFromJson(key, chapterElement))
      }
    }
    return chapters
  }

  override fun fetchPageList(chapter: ChapterInfo): List<PageInfo> {
    TODO()
  }

  private fun removeMangaNameFromUrl(url: String): String = url.substringBeforeLast("/") + "/"

  private fun getMangaId(url: String): String {
    val lastSection = url.trimEnd('/').substringAfterLast("/")
    return if (lastSection.toIntOrNull() != null) {
      lastSection
    } else {
      //this occurs if person has manga from before that had the id/name/
      url.trimEnd('/').substringBeforeLast("/").substringAfterLast("/")
    }
  }

  //remove bbcode as well as parses any html characters in description or chapter name to actual characters for example &hearts will show a heart
  private fun cleanString(description: String): String {
    return Jsoup.parseBodyFragment(description.replace("[list]", "").replace("[/list]", "").replace("[*]", "").replace("""\[(\w+)[^\]]*](.*?)\[/\1]""".toRegex(), "$2")).text()
  }

  private fun chapterFromJson(chapterId: String, chapterJson: JsonObject): ChapterInfo {
    val key = BASE_CHAPTER + chapterId
    var chapterName = mutableListOf<String>()
    //build chapter name
    if (chapterJson.get("volume").string.isNotBlank()) {
      chapterName.add("Vol." + chapterJson.get("volume").string)
    }
    if (chapterJson.get("chapter").string.isNotBlank()) {
      chapterName.add("Ch." + chapterJson.get("chapter").string)
    }
    if (chapterJson.get("title").string.isNotBlank()) {
      chapterName.add("-")
      chapterName.add(chapterJson.get("title").string)
    }

    val name = cleanString(chapterName.joinToString(" "))
    //convert from unix time
    val dateUpload = chapterJson.get("timestamp").long * 1000
    var scanlatorName = mutableListOf<String>()
    if (!chapterJson.get("group_name").nullString.isNullOrBlank()) {
      scanlatorName.add(chapterJson.get("group_name").string)
    }
    if (!chapterJson.get("group_name_2").nullString.isNullOrBlank()) {
      scanlatorName.add(chapterJson.get("group_name_2").string)
    }
    if (!chapterJson.get("group_name_3").nullString.isNullOrBlank()) {
      scanlatorName.add(chapterJson.get("group_name_3").string)
    }
    val scanlator = scanlatorName.joinToString(" & ")

    return ChapterInfo(
      key = key,
      name = name,
      dateUpload = dateUpload,
      scanlator = scanlator
    )
  }

//  override fun imageUrlParse(document: Document): String = ""

  private fun parseStatus(status: Int) = when (status) {
    1 -> MangaInfo.ONGOING
    2 -> MangaInfo.COMPLETED
    else -> MangaInfo.UNKNOWN
  }

  private fun getImageUrl(attr: String): String {
    //some images are hosted elsewhere
    if (attr.startsWith("http")) {
      return attr
    }
    return baseUrl + attr
  }

  override fun getListings(): List<Listing> {
    return listOf(Latest())
  }

  override fun getFilters(): FilterList {
    return listOf(
      Filter.Title(),
      Filter.Author(),
      Filter.Artist(),
      R18(),
      GenreList(getGenreList())
    )
  }

  inner class Latest : Listing("Latest")

  private class TextField(name: String, val key: String) : Filter.Text(name)
  private class Genre(val id: String, name: String) : Filter.Check(name)
  private class GenreList(genres: List<Genre>) : Filter.Group("Genres", genres)
  private class R18 : Filter.Select("R18+", arrayOf("Show all", "Show only", "Show none"))

  private fun getFilterList() = listOf(
    TextField("Author", "author"),
    TextField("Artist", "artist"),
    R18(),
    GenreList(getGenreList())
  )

  private fun getGenreList() = listOf(
    Filter.Genre("4-koma"),
    Filter.Genre("Action"),
    Filter.Genre("Adventure"),
    Filter.Genre("Award Winning"),
    Filter.Genre("Comedy"),
    Filter.Genre("Cooking"),
    Filter.Genre("Doujinshi"),
    Filter.Genre("Drama"),
    Filter.Genre("Ecchi"),
    Filter.Genre("Fantasy"),
    Filter.Genre("Gender Bender"),
    Filter.Genre("Harem"),
    Filter.Genre("Historical"),
    Filter.Genre("Horror"),
    Filter.Genre("Josei"),
    Filter.Genre("Martial Arts"),
    Filter.Genre("Mecha"),
    Filter.Genre("Medical"),
    Filter.Genre("Music"),
    Filter.Genre("Mystery"),
    Filter.Genre("Oneshot"),
    Filter.Genre("Psychological"),
    Filter.Genre("Romance"),
    Filter.Genre("School Life"),
    Filter.Genre("Sci-Fi"),
    Filter.Genre("Seinen"),
    Filter.Genre("Shoujo"),
    Filter.Genre("Shoujo Ai"),
    Filter.Genre("Shounen"),
    Filter.Genre("Shounen Ai"),
    Filter.Genre("Slice of Life"),
    Filter.Genre("Smut"),
    Filter.Genre("Sports"),
    Filter.Genre("Supernatural"),
    Filter.Genre("Tragedy"),
    Filter.Genre("Webtoon"),
    Filter.Genre("Yaoi"),
    Filter.Genre("Yuri"),
    Filter.Genre("[no chapters]"),
    Filter.Genre("Game"),
    Filter.Genre("Isekai")
  )

  override fun handlesLink(url: String): DeepLink? {
    return when {
      "/chapter/" in url ->
        DeepLink.Chapter("/chapter/" + url.substringAfter("chapter/").substringBefore("/"))
      "/manga/" in url -> DeepLink.Manga(url.substringAfter("mangadex.org")
        .substringBeforeLast("/") + "/")
      else -> null
    }
  }

  override fun findMangaKey(chapterKey: String): String? {
    val request = GET(baseUrl + chapterKey, headers)
    val response = client.newCall(request).execute()
    val body = response.body()?.string() ?: return null

    val document = Jsoup.parse(body)
    val mangaId = document.getElementsByTag("meta")
      .first { it.attr("property") == "og:image" }
      .attr("content")
      .substringAfterLast("/")
      .substringBefore(".")

    return "/title/$mangaId"
  }

  companion object {
    //this number matches to the cookie
    private const val NO_R18 = 0
    private const val ALL = 1
    private const val ONLY_R18 = 2
    private const val API_MANGA = "/api/manga/"
    private const val BASE_CHAPTER = "/chapter/"

  }
}
