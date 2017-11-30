package eu.kanade.tachiyomi.source

import android.content.Context
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.english.*
import eu.kanade.tachiyomi.source.online.german.WieManga
import eu.kanade.tachiyomi.source.online.russian.Mangachan
import eu.kanade.tachiyomi.source.online.russian.Mintmanga
import eu.kanade.tachiyomi.source.online.russian.Readmanga

open class SourceManager(private val context: Context) {

    private val sourcesMap = mutableMapOf<Long, Source>()

    init {
        createInternalSources().forEach { registerSource(it) }
    }

    open fun get(sourceKey: Long): Source? {
        return sourcesMap[sourceKey]
    }

    fun getOnlineSources() = sourcesMap.values.filterIsInstance<HttpSource>()

    fun getCatalogueSources() = sourcesMap.values.filterIsInstance<CatalogueSource>()

    internal fun registerSource(source: Source, overwrite: Boolean = false) {
        if (overwrite || !sourcesMap.containsKey(source.id)) {
            sourcesMap.put(source.id, source)
        }
    }

    internal fun unregisterSource(source: Source) {
        sourcesMap.remove(source.id)
    }

    private fun createInternalSources(): List<Source> = listOf(
            LocalSource(context),
            Batoto(),
            Mangahere(),
            Mangafox(),
            Kissmanga(),
            Readmanga(),
            Mintmanga(),
            Mangachan(),
            Readmangatoday(),
            Mangasee(),
            WieManga()
    )
}
