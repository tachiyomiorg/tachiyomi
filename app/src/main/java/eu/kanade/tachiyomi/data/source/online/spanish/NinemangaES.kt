package eu.kanade.tachiyomi.data.source.online.spanish

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.source.ES
import eu.kanade.tachiyomi.data.source.Language
import eu.kanade.tachiyomi.data.source.online.multi.Ninemanga

class NinemangaES(context: Context, override val id: Int) : Ninemanga(context) {

    override val lang: Language = ES

    override val baseUrl: String = "http://${lang.code}.ninemanga.com"

    override fun parseStatus(status: String) = when {
        status.contains("(Completo") -> Manga.COMPLETED
        else -> Manga.UNKNOWN
    }

}