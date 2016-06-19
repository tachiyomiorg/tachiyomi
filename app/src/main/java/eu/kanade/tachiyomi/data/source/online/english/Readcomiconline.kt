package eu.kanade.tachiyomi.data.source.online.english

import android.content.Context

class Readcomiconline(context: Context, override val id: Int) : Kissmanga(context, id) {

    override val name = "ReadComicOnline"

    override val baseUrl = "http://readcomiconline.to"

    override fun popularMangaInitialUrl() = "$baseUrl/ComicList/MostPopular"

}
