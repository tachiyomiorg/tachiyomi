package tachiyomi.source

import tachiyomi.source.model.ChapterInfo
import tachiyomi.source.model.Filter
import tachiyomi.source.model.FilterList
import tachiyomi.source.model.Listing
import tachiyomi.source.model.MangaInfo
import tachiyomi.source.model.MangasPageInfo
import tachiyomi.source.model.PageInfo

class TestSource : CatalogSource {
  override val id = 1L

  override val name = "Test source"
  override val lang get() = "en"
  override fun fetchMangaDetails(manga: MangaInfo): MangaInfo {
    Thread.sleep(1000)
    val noHipstersOffset = 10
    val picId = manga.title.split(" ")[1].toInt() + noHipstersOffset
    return manga.copy(cover = "https://picsum.photos/300/400/?image=$picId", initialized = true)
  }

  override fun fetchMangaList(sort: Listing?, page: Int): MangasPageInfo {
//    Thread.sleep(3000)
    return MangasPageInfo(getTestManga(page), true)
  }

  override fun fetchMangaList(filters: FilterList, page: Int): MangasPageInfo {
    var mangaList = getTestManga(page)

    filters.forEach { filter ->
      if (filter is Filter.Title) {
        mangaList = mangaList.filter { filter.value in it.title }
      }
    }

    return MangasPageInfo(mangaList, true)
  }

  override fun fetchChapterList(manga: MangaInfo): List<ChapterInfo> {
    Thread.sleep(1000)
    return getTestChapters()
  }

  override fun fetchPageList(chapter: ChapterInfo): List<PageInfo> {
    Thread.sleep(1000)
    return getTestPages()
  }

  class Alphabetically : Listing("Alphabetically")

  class Latest : Listing("Latest")

  override fun getListings(): List<Listing> {
    return listOf(Alphabetically(), Latest())
  }

  override fun getFilters(): FilterList {
    return listOf(
      Filter.Title(),
      Filter.Author(),
      Filter.Artist(),
      GenreList(getGenreList())
    )
  }

  //  private class Status : Filter.Check("Completed")
//  private class StatusValue(filter: Status) : Filter.Check(filter, null)
//  private class Author : Filter.Text("Author")
//  private class Genre(name: String) : Filter.TriState(name)
  private class GenreList(genres: List<Filter.Genre>) : Filter.Group("Genres", genres)

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

  private fun getTestManga(page: Int): List<MangaInfo> {
    val list = mutableListOf<MangaInfo>()
    val id = (page - 1) * 20 + 1
    val manga1 = MangaInfo(
      "$id",
      "Manga $id",
      "",
      "",
      "",
      "",
      0,
      "",
      false
    )
    list += manga1

    for (i in 1..19) {
      list += manga1.copy(key = "${id + i}", title = "Manga ${id + i}")
    }

    return list
  }

  private fun getTestChapters(): List<ChapterInfo> {
    val chapter1 = ChapterInfo(
      "1",
      "Chapter 1",
      System.currentTimeMillis()
    )
    val chapter2 = chapter1.copy(key = "2", name = "Chapter2")
    val chapter3 = chapter1.copy(key = "3", name = "Chapter3")

    return listOf(chapter1, chapter2, chapter3)
  }

  private fun getTestPages(): List<PageInfo> {
    return listOf(
      PageInfo("url1", "imageUrl1"),
      PageInfo("url2", "imageUrl2")
    )
  }

}
