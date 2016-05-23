package eu.kanade.tachiyomi.data.source

import android.content.Context
import android.os.Environment
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.source.base.OnlineSource
import eu.kanade.tachiyomi.data.source.base.Source
import eu.kanade.tachiyomi.data.source.base.YamlOnlineSource
import eu.kanade.tachiyomi.data.source.online.english.Batoto
import eu.kanade.tachiyomi.data.source.online.english.Kissmanga
import eu.kanade.tachiyomi.data.source.online.english.Mangafox
import eu.kanade.tachiyomi.data.source.online.english.Mangahere
import org.yaml.snakeyaml.Yaml
import timber.log.Timber
import java.io.File

open class SourceManager(private val context: Context) {

    val BATOTO = 1
    val MANGAHERE = 2
    val MANGAFOX = 3
    val KISSMANGA = 4
    val READMANGA = 5
    val MINTMANGA = 6
    val MANGACHAN = 7
    val READMANGATODAY = 8

    val LAST_SOURCE = 8

    val sourcesMap = createSources()

    open fun get(sourceKey: Int): Source? {
        return sourcesMap[sourceKey]
    }

    fun getOnlineSources() = sourcesMap.values.filterIsInstance(OnlineSource::class.java)

    private fun createSource(id: Int): Source? = when (id) {
        BATOTO -> Batoto(context, id)
        KISSMANGA -> Kissmanga(context, id)
        MANGAHERE -> Mangahere(context, id)
        MANGAFOX -> Mangafox(context, id)
        else -> null
    }

    private fun createSources(): Map<Int, Source> = hashMapOf<Int, Source>().apply {
        for (i in 1..LAST_SOURCE) {
            createSource(i)?.let { put(i, it) }
        }

        val parsersDir = File(Environment.getExternalStorageDirectory().absolutePath +
                File.separator + context.getString(R.string.app_name), "parsers")

        if (parsersDir.exists()) {
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
