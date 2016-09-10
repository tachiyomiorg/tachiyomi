package eu.kanade.tachiyomi.data.source

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.content.Context
import android.os.Environment
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.source.online.OnlineSource
import eu.kanade.tachiyomi.data.source.online.YamlOnlineSource
import eu.kanade.tachiyomi.data.source.online.english.*
import eu.kanade.tachiyomi.data.source.online.german.*
import eu.kanade.tachiyomi.data.source.online.italian.*
import eu.kanade.tachiyomi.data.source.online.portuguese.*
import eu.kanade.tachiyomi.data.source.online.russian.*
import eu.kanade.tachiyomi.data.source.online.spanish.*
import eu.kanade.tachiyomi.util.hasPermission
import org.yaml.snakeyaml.Yaml
import timber.log.Timber
import java.io.File

open class SourceManager(private val context: Context) {

    val SOURCES = 19

    val sourcesMap = createSources()

    open fun get(sourceKey: Int): Source? {
        return sourcesMap[sourceKey]
    }

    fun getOnlineSources() = sourcesMap.values.filterIsInstance(OnlineSource::class.java)

    private fun createSource(id: Int): Source? = when (id) {
        1 -> Batoto(context, id)
        2 -> MangahereEN(context, id)
        3 -> Mangafox(context, id)
        4 -> Kissmanga(context, id)
        5 -> Readmanga(context, id)
        6 -> Mintmanga(context, id)
        7 -> Mangachan(context, id)
        8 -> Readmangatoday(context, id)
        9 -> Mangasee(context, id)
        10 -> WieManga(context, id)
        11 -> NinemangaEN(context, id)
        12 -> NinemangaDE(context, id)
        13 -> NinemangaIT(context, id)
        14 -> NinemangaBR(context, id)
        15 -> NinemangaRU(context, id)
        16 -> NinemangaES(context, id)
        17 -> MangahereES(context, id)
        18 -> MangaedenEN(context, id)
        19 -> MangaedenIT(context, id)
        else -> null
    }

    private fun createSources(): Map<Int, Source> = hashMapOf<Int, Source>().apply {
        for (i in 1..SOURCES) {
            createSource(i)?.let { put(i, it) }
        }

        val parsersDir = File(Environment.getExternalStorageDirectory().absolutePath +
                File.separator + context.getString(R.string.app_name), "parsers")

        if (parsersDir.exists() && context.hasPermission(READ_EXTERNAL_STORAGE)) {
            val yaml = Yaml()
            for (file in parsersDir.listFiles().filter { it.extension == "yml" }) {
                try {
                    val map = file.inputStream().use { yaml.loadAs(it, Map::class.java) }
                    YamlOnlineSource(context, map).let { put(it.id, it) }
                } catch (e: Exception) {
                    Timber.e("Error loading source from file. Bad format?")
                }
            }
        }
    }

}
