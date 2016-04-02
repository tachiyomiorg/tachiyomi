package eu.kanade.tachiyomi.data.source

import android.content.Context
import android.os.Environment
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.source.base.OnlineSource
import eu.kanade.tachiyomi.data.source.base.Source
import eu.kanade.tachiyomi.data.source.base.YamlParsedOnlineSource
import eu.kanade.tachiyomi.data.source.online.english.Batoto
import eu.kanade.tachiyomi.data.source.online.english.Kissmanga
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

    private fun createSource(id: Int): Source? = when (id) {
        BATOTO -> Batoto(context, id)
        KISSMANGA -> Kissmanga(context, id)
        else -> null
    }

    fun getOnlineSources() = sourcesMap.values.filterIsInstance(OnlineSource::class.java)

    fun createSources(): Map<Int, Source> = hashMapOf<Int, Source>().apply {
        for (i in 1..LAST_SOURCE) {
            createSource(i)?.let { put(i, it) }
        }

        val parsersDir = File(Environment.getExternalStorageDirectory().absolutePath +
                File.separator + context.getString(R.string.app_name), "parsers")

        if (parsersDir.exists()) {
            for (file in parsersDir.listFiles { file, filename -> filename.endsWith(".yml") }) {
                try {
                    YamlParsedOnlineSource(context, file.inputStream()).let { put(it.id, it) }
                } catch (e: Exception) {
                    Timber.e("Error loading source from file. Bad format?")
                }
            }
        }
    }

}
