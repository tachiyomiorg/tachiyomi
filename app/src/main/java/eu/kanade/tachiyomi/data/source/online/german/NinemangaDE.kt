package eu.kanade.tachiyomi.data.source.online.german

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.source.DE
import eu.kanade.tachiyomi.data.source.Language
import eu.kanade.tachiyomi.data.source.online.multi.Ninemanga

class NinemangaDE(context: Context, override val id: Int) : Ninemanga(context) {

    override val lang: Language = DE

    override val baseUrl: String = "http://${lang.code}.ninemanga.com"

    override fun parseStatus(status: String) = when {
        status.contains("Laufende") -> Manga.ONGOING
        status.contains("Abgeschlossen") -> Manga.COMPLETED
        else -> Manga.UNKNOWN
    }

}
