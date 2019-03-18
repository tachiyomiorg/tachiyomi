package eu.kanade.tachiyomi.data.track

import android.content.Context
import eu.kanade.tachiyomi.data.track.anilist.Anilist
import eu.kanade.tachiyomi.data.track.kitsu.Kitsu
import eu.kanade.tachiyomi.data.track.myanimelist.Myanimelist
import eu.kanade.tachiyomi.data.track.shikomori.Shikomori

class TrackManager(private val context: Context) {

    companion object {
        const val MYANIMELIST = 1
        const val ANILIST = 2
        const val KITSU = 3
        const val SHIKOMORI = 4
    }

    val myAnimeList = Myanimelist(context, MYANIMELIST)

    val aniList = Anilist(context, ANILIST)

    val kitsu = Kitsu(context, KITSU)

    val shikomori = Shikomori(context, SHIKOMORI)

    val services = listOf(myAnimeList, aniList, kitsu, shikomori)

    fun getService(id: Int) = services.find { it.id == id }

    fun hasLoggedServices() = services.any { it.isLogged }

}
