package eu.kanade.tachiyomi.source

import android.graphics.drawable.Drawable
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.toChapterInfo
import eu.kanade.tachiyomi.source.model.toMangaInfo
import eu.kanade.tachiyomi.source.model.toPageUrl
import eu.kanade.tachiyomi.source.model.toSChapter
import eu.kanade.tachiyomi.source.model.toSManga
import eu.kanade.tachiyomi.util.lang.awaitSingle
import rx.Observable
import tachiyomi.source.model.ChapterInfo
import tachiyomi.source.model.MangaInfo
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * A basic interface for creating a source. It could be an online source, a local source, etc...
 */
interface Source : tachiyomi.source.Source {

    /**
     * Id for the source. Must be unique.
     */
    override val id: Long

    /**
     * Name of the source.
     */
    override val name: String

    override val lang: String
        get() = ""

    /**
     * Returns an observable with the updated details for a manga.
     *
     * @param manga the manga to update.
     */
    @Deprecated("Use getMangaDetails instead")
    fun fetchMangaDetails(manga: SManga): Observable<SManga>

    /**
     * Returns an observable with all the available chapters for a manga.
     *
     * @param manga the manga to update.
     */
    @Deprecated("Use getChapterList instead")
    fun fetchChapterList(manga: SManga): Observable<List<SChapter>>

    /**
     * Returns an observable with the list of pages a chapter has.
     *
     * @param chapter the chapter.
     */
    @Deprecated("Use getPageList instead")
    fun fetchPageList(chapter: SChapter): Observable<List<Page>>

    /**
     * [1.x API] Get the updated details for a manga.
     */
    @Suppress("DEPRECATION")
    override suspend fun getMangaDetails(manga: MangaInfo): MangaInfo {
        val sManga = manga.toSManga()
        val networkManga = fetchMangaDetails(sManga).awaitSingle()
        sManga.copyFrom(networkManga)
        return sManga.toMangaInfo()
    }

    /**
     * [1.x API] Get all the available chapters for a manga.
     */
    @Suppress("DEPRECATION")
    override suspend fun getChapterList(manga: MangaInfo): List<ChapterInfo> {
        return fetchChapterList(manga.toSManga()).awaitSingle()
            .map { it.toChapterInfo() }
    }

    /**
     * [1.x API] Get the list of pages a chapter has.
     */
    @Suppress("DEPRECATION")
    override suspend fun getPageList(chapter: ChapterInfo): List<tachiyomi.source.model.Page> {
        return fetchPageList(chapter.toSChapter()).awaitSingle()
            .map { it.toPageUrl() }
    }
}

fun Source.icon(): Drawable? = Injekt.get<ExtensionManager>().getAppIconForSource(this)

fun Source.getPreferenceKey(): String = "source_$id"
