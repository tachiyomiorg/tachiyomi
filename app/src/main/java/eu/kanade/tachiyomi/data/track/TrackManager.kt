package eu.kanade.tachiyomi.data.track

import android.content.Context
import eu.kanade.tachiyomi.data.track.anilist.Anilist
import eu.kanade.tachiyomi.data.track.kitsu.Kitsu
import eu.kanade.tachiyomi.data.track.myanimelist.Myanimelist
import eu.kanade.tachiyomi.data.track.shikimori.Shikimori

class TrackManager(private val context: Context) {

    companion object {
        const val MYANIMELIST = 1
        const val ANILIST = 2
        const val KITSU = 3
        const val SHIKIMORI = 4
    }

    val myAnimeList = Myanimelist(context, MYANIMELIST)

    val aniList = Anilist(context, ANILIST)

    val kitsu = Kitsu(context, KITSU)

    val shikimori = Shikimori(context, SHIKIMORI)

    val services = listOf(myAnimeList, aniList, kitsu, shikimori)

    fun getService(id: Int) = services.find { it.id == id }

    fun hasLoggedServices() = services.any { it.isLogged }

}
