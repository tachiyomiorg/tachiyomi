package eu.kanade.tachiyomi.data.source

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.content.Context
import android.os.Environment
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.source.online.OnlineSource
import eu.kanade.tachiyomi.data.source.online.YamlOnlineSource
import eu.kanade.tachiyomi.data.source.online.english.*
import eu.kanade.tachiyomi.data.source.online.german.WieManga
import eu.kanade.tachiyomi.data.source.online.russian.Mangachan
import eu.kanade.tachiyomi.data.source.online.russian.Mintmanga
import eu.kanade.tachiyomi.data.source.online.russian.Readmanga
import eu.kanade.tachiyomi.util.hasPermission
import org.yaml.snakeyaml.Yaml
import timber.log.Timber
import java.io.File
import dalvik.system.DexClassLoader

open class SourceManager(private val context: Context) {

    private val sourcesMap = createSources()

    open fun get(sourceKey: Int): Source? {
        return sourcesMap[sourceKey]
    }

    fun getOnlineSources() = sourcesMap.values.filterIsInstance(OnlineSource::class.java)

    private fun createOnlineSourceList(): List<Source> = listOf(
            Batoto(1),
            Mangahere(2),
            Mangafox(3),
            Kissmanga(4),
            Readmanga(5),
            Mintmanga(6),
            Mangachan(7),
            Readmangatoday(8),
            Mangasee(9),
            WieManga(10)
    )

    private fun createSources(): Map<Int, Source> = hashMapOf<Int, Source>().apply {
        createOnlineSourceList().forEach { put(it.id, it) }

        val parsersDir = File(Environment.getExternalStorageDirectory().absolutePath +
                File.separator + context.getString(R.string.app_name), "parsers")

        if (parsersDir.exists() && context.hasPermission(READ_EXTERNAL_STORAGE)) {
            val yaml = Yaml()
            for (file in parsersDir.listFiles()) {
                try {
                    if (file.extension == "yml") {
                        val map = file.inputStream().use { yaml.loadAs(it, Map::class.java) }
                        YamlOnlineSource(map).let { put(it.id, it) }
                    } else if (file.extension == "jar") {
                        val cl = DexClassLoader(file.absolutePath, context.getDir("outdex", Context.MODE_PRIVATE).absolutePath, null, context.classLoader)
                        val clazz = cl.loadClass(file.name.dropLast(4))
                        val source = clazz.newInstance() as Source
                        put(source.id, source)
                    }
                } catch (e: Exception) {
                    Timber.e("Error loading source from file. Bad format?")
                }
            }
        }
    }

}