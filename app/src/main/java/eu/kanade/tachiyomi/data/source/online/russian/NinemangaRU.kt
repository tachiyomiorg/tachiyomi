package eu.kanade.tachiyomi.data.source.online.russian

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.source.Language
import eu.kanade.tachiyomi.data.source.RU
import eu.kanade.tachiyomi.data.source.online.multi.Ninemanga

class NinemangaRU(context: Context, override val id: Int) : Ninemanga(context) {

    override val lang: Language = RU

    override val baseUrl: String = "http://${lang.code}.ninemanga.com"

    override fun parseStatus(status: String) = when {
        else -> Manga.UNKNOWN
    }

}
