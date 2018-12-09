package tachiyomi.source

import tachiyomi.source.model.ChapterInfo
import tachiyomi.source.model.MangaInfo
import tachiyomi.source.model.PageInfo

/**
 * A basic interface for creating a source. It could be an online source, a local source, etc...
 */
interface Source {

  /**
   * Id for the source. Must be unique.
   */
  val id: Long

  /**
   * Name of the source.
   */
  val name: String

  /**
   * Returns an observable with the updated details for a manga.
   *
   * @param manga the manga to update.
   */
  fun fetchMangaDetails(manga: MangaInfo): MangaInfo

  /**
   * Returns an observable with all the available chapters for a manga.
   *
   * @param manga the manga to update.
   */
  fun fetchChapterList(manga: MangaInfo): List<ChapterInfo>

  /**
   * Returns an observable with the list of pages a chapter has.
   *
   * @param chapter the chapter.
   */
  fun fetchPageList(chapter: ChapterInfo): List<PageInfo>

  /**
   * Returns a regex used to determine chapter information.
   *
   * @return empty regex will run default parser.
   */
  fun getRegex(): Regex {
    return Regex("")
  }

}
