package eu.kanade.tachiyomi.source.online.english

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import rx.Observable

class Batoto : Source {

    override val id: Long = 1

    override val name = "Batoto"

    override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
        return Observable.error(Exception("RIP Batoto"))
    }

    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> {
        return Observable.error(Exception("RIP Batoto"))
    }

    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
        return Observable.error(Exception("RIP Batoto"))
    }

    override fun toString(): String {
        return "$name (EN)"
    }

}
