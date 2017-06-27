package eu.kanade.tachiyomi.source.online.english

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.*
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.*
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import rx.Observable
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import java.util.concurrent.TimeUnit

class Mangazuki : ParsedHttpSource() {

    override val id: Long = 11

    override val name = "Mangazuki"

    override val baseUrl = "https://mangazuki.co"

    override val lang = "en"

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient

    override fun latestUpdatesSelector() = ".page-content .row .thumbnail"

    override fun popularMangaSelector() = latestUpdatesSelector()

    override fun latestUpdatesNextPageSelector() = "ul.pagination li.next:not(.disabled) a"

    override fun popularMangaNextPageSelector() = latestUpdatesNextPageSelector()

    // Mangazuki's mangas sorted by popularity is 8 recommended mangas on the frontpage.
    // As such I think the alphabetical list of all series is a better choice.
    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/series?page=$page", headers)
    }

    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/latest?page=$page", headers)
    }

    override fun popularMangaFromElement(element: Element): SManga {
        val manga = SManga.create()
        element.select(".caption a").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.text()
        }
        element.select(".thumb img").first().let {
            manga.thumbnail_url = baseUrl + it.attr("src")
        }
        return manga
    }

    override fun latestUpdatesFromElement(element: Element): SManga {
        return popularMangaFromElement(element)
    }

    override fun latestUpdatesParse(response: Response): MangasPage {
        val mp = super.latestUpdatesParse(response)

        // Mangazuki posts an entry for every chapter added, this is a bit confusing in tachiyomi.
        // The best I could do without overhauling how the rest of the app works is to remove
        // duplicates from a single page of results.
        val map = HashMap<String, Boolean>()
        val uniqueMangas = mp.mangas.filter({ m ->
            if(!map.containsKey(m.url)){
                map.put(m.url, true)
                true
            }
            else{
                false
            }
        })

        return MangasPage(uniqueMangas, mp.hasNextPage)
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        // Use a GET request, because the POST request requires CSRF tokens.
        val baseUrl = HttpUrl.parse("$baseUrl/series");
        val url = baseUrl!!.newBuilder()
                .addQueryParameter("q", query)
                .addQueryParameter("page", page.toString())
                .toString()
        return GET(url, headers);
    }

    override fun searchMangaSelector() = latestUpdatesSelector()

    override fun searchMangaFromElement(element: Element): SManga {
        return popularMangaFromElement(element)
    }

    override fun searchMangaNextPageSelector() = latestUpdatesNextPageSelector()

    override fun mangaDetailsParse(document: Document): SManga {
        val infoElement = document.select(".col-lg-4 .panel-body").first()

        val manga = SManga.create()
        manga.description = infoElement.select("p").text()
        manga.thumbnail_url = baseUrl + infoElement.select(".img-circle").attr("src")
        return manga
    }

    override fun pageListParse(document: Document): List<Page> {
        var i = 0;
        val pages = mutableListOf<Page>()
        document.select(".page-content .row .img-lazy").forEach {
            pages.add(Page(i, it.attr("data-src"), it.attr("data-src")))
            i++;
        }
        return pages
    }

    override fun chapterListSelector() = "ul.media-list li.media"

    override fun chapterFromElement(element: Element): SChapter {
        val urlElement = element.select("a").first()

        val chapter = SChapter.create()
        chapter.setUrlWithoutDomain(urlElement.attr("href"))
        chapter.name = urlElement.select(".media-heading").text()

        // Only a "X days, Y hours ago" or string date is available.
        chapter.date_upload = 0
        return chapter
    }

    // Mangazuki has pagination for their chapter list, which the app does not account for with this method.
    // Use some RX observable chain to request each page and reduce them into one List.
    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> {
        var pageNum = 1
        // Defer creates a new observable on every subscribe, therefore incrementing pageNum.
        val pageReqGen = Observable.defer { -> fetchChapterListPage(pageNum++, manga) }

        // We need something to combine our page results into that we can call ourselves, PublishSubject.
        val accum = PublishSubject.create<ChapterListPage>()

        // Every page that comes in, will check if a new page should be requested.
        accum.subscribe{ clp ->
            if(clp.hasNext){
                pageReqGen
                    .observeOn(Schedulers.io())
                        //This delay is important, so the callback happens outside this context.
                    .delay(10, TimeUnit.MILLISECONDS)
                    .subscribe { clp ->
                        accum.onNext(clp)
                        // Do the onComplete call here, we need to know onNext has finished.
                        if(!clp.hasNext){
                            accum.onCompleted()
                        }
                    }
            }
        }

        //Start the loop by giving a dummy result.
        accum.onNext(ChapterListPage(emptyList(), true))

        //Use reduce to combine the intermediate results into a single list.
        return accum
                .reduce(emptyList<SChapter>(), { a, b -> a + b.chapters })
                .asObservable()
    }

    // The request method for a single page.
    private fun fetchChapterListPage(page: Int, manga: SManga): Observable<ChapterListPage> {
        return client.newCall(GET(baseUrl + manga.url + "?page=$page"))
                .asObservableSuccess()
                .map { response -> chapterListPageParse(response) }
    }

    // Simple structure to hold our intermediate results.
    private class ChapterListPage(val chapters: List<SChapter>, val hasNext: Boolean)

    // Because we need to know if a next page exists, a different parse method is required.
    private fun chapterListPageParse(response : Response): ChapterListPage {
        val document = response.asJsoup()
        val clp = ChapterListPage(
                document.select(chapterListSelector()).map { chapterFromElement(it) },
                document.select(".panel-footer ul.pagination li.next:not(.disabled) a").count() > 0
        )
        return clp
    }

    override fun imageUrlParse(document: Document) = ""

    override fun getFilterList() = FilterList()
}