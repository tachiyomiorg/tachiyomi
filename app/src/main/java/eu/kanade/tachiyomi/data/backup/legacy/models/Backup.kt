package eu.kanade.tachiyomi.data.backup.legacy.models

import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.Track
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class Backup(
    val version: Int? = null,
    var mangas: MutableList<MangaObject> = mutableListOf(),
    var categories: List<@Contextual Category>? = null,
    var extensions: List<String>? = null
) {
    companion object {
        const val CURRENT_VERSION = 2

        fun getDefaultFilename(): String {
            val date = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
            return "tachiyomi_$date.json"
        }
    }
}

@Serializable
data class MangaObject(
    var manga: @Contextual Manga,
    var chapters: List<@Contextual Chapter>? = null,
    var categories: List<String>? = null,
    var track: List<@Contextual Track>? = null,
    var history: List<@Contextual DHistory>? = null
)
