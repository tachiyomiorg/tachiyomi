package eu.kanade.tachiyomi.data.source.online.italian

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.source.IT
import eu.kanade.tachiyomi.data.source.Language
import eu.kanade.tachiyomi.data.source.online.multi.Ninemanga

class NinemangaIT(context: Context, override val id: Int) : Ninemanga(context) {

    override val lang: Language = IT

    override val baseUrl: String = "http://${lang.code}.ninemanga.com"

    override fun parseStatus(status: String) = when {
        status.contains("In corso") -> Manga.ONGOING
        status.contains("Completado") -> Manga.COMPLETED
        else -> Manga.UNKNOWN
    }

}
