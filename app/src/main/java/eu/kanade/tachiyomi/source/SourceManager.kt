package eu.kanade.tachiyomi.source

import android.content.Context
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.english.*
import eu.kanade.tachiyomi.source.online.german.WieManga
import eu.kanade.tachiyomi.source.online.russian.Mangachan
import eu.kanade.tachiyomi.source.online.russian.Mintmanga
import eu.kanade.tachiyomi.source.online.russian.Readmanga
import rx.Observable

open class SourceManager(private val context: Context) {

    private val sourcesMap = mutableMapOf<Long, Source>()
    private val stubSourcesMap = mutableMapOf<Long, Source>(1L to stubSource(1L, "Batoto", "Batoto (EN)", "RIP Batoto"))

    init {
        createInternalSources().forEach { registerSource(it) }
    }

    open fun get(sourceKey: Long): Source {
        var source = sourcesMap[sourceKey] ?: stubSourcesMap[sourceKey]
        if (source == null) {
            val name = sourceKey.toString()
            source = stubSource(sourceKey, name, name, context.getString(R.string.source_not_installed, name))
            stubSourcesMap[sourceKey] = source
        }
        return source
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

    private class stubSource(override val id: Long, override val name : String, val toStr: String, var error : String) : Source {
        override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
            return Observable.error(Exception(error))
        }

        override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> {
            return Observable.error(Exception(error))
        }

        override fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
            return Observable.error(Exception(error))
        }

        override fun toString(): String {
            return toStr
        }
    }
}
