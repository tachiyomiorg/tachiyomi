package eu.kanade.tachiyomi.data.track.bangumi

data class Collection(
  val `private`: Int? = 0,
  val comment: String? = "",
  val ep_status: Int? = 0,
  val lasttouch: Int? = 0,
  val rating: Int? = 0,
  val status: Status? = Status(),
  val tag: List<String?>? = listOf(),
  val user: User? = User(),
  val vol_status: Int? = 0
)