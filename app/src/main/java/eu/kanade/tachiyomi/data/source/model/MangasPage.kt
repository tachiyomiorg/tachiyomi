package eu.kanade.tachiyomi.data.source.model

class MangasPage(val page: Int) {

    val mangas: MutableList<SManga> = mutableListOf()

    lateinit var url: String

    var nextPageUrl: String? = null

}
