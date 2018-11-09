package tachiyomi.source.model

/**
 * Model for a manga given by a source
 */
data class MangaInfo(
  val key: String,
  val title: String,
  val artist: String = "",
  val author: String = "",
  val description: String = "",
  val genres: String = "",
  val status: Int = UNKNOWN,
  val cover: String = "",
  val initialized: Boolean = false
) {

  companion object {
    const val UNKNOWN = 0
    const val ONGOING = 1
    const val COMPLETED = 2
    const val LICENSED = 3
    const val PUBLISHING_FINISHED = 4
  }
}
