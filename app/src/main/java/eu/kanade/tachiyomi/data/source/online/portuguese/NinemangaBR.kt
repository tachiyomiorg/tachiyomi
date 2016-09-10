package eu.kanade.tachiyomi.data.source.online.portuguese

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.source.BR
import eu.kanade.tachiyomi.data.source.Language
import eu.kanade.tachiyomi.data.source.online.multi.Ninemanga

class NinemangaBR(context: Context, override val id: Int) : Ninemanga(context) {

    override val lang: Language = BR

    override val baseUrl: String = "http://${lang.code}.ninemanga.com"

    override fun parseStatus(status: String) = when {
        else -> Manga.UNKNOWN
    }

}
