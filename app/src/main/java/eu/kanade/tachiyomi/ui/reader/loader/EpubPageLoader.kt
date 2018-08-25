package eu.kanade.tachiyomi.ui.reader.loader

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import rx.Observable
import java.io.File
import java.util.zip.ZipFile

/**
 * Loader used to load a chapter from a .epub file.
 */
class EpubPageLoader(file: File) : PageLoader() {

    /**
     * The epub loaded as a zip file.
     */
    private val zip = ZipFile(file)

    /**
     * Recycles this loader and the open zip.
     */
    override fun recycle() {
        super.recycle()
        zip.close()
    }

    /**
     * Returns an observable containing the pages found on this zip archive ordered with a natural
     * comparator.
     */
    override fun getPages(): Observable<List<ReaderPage>> {
        val allEntries = zip.entries().toList()
        val ref = getPackageHref(zip)
        val doc = getPackageDocument(zip, ref)
        val pages = getPagesFromDocument(doc)
        val hrefs = getHrefMap(ref, allEntries.map { it.name })
        return getImagesFromPages(zip, pages, hrefs)
            .mapIndexed { i, path ->
                val streamFn = { zip.getInputStream(zip.getEntry(path)) }
                ReaderPage(i).apply {
                    stream = streamFn
                    status = Page.READY
                }
            }
            .let { Observable.just(it) }
    }

    /**
     * Returns an observable that emits a ready state unless the loader was recycled.
     */
    override fun getPage(page: ReaderPage): Observable<Int> {
        return Observable.just(if (isRecycled) {
            Page.ERROR
        } else {
            Page.READY
        })
    }

    /**
     * Returns the path to the package document.
     */
    private fun getPackageHref(zip: ZipFile): String {
        val meta = zip.getEntry("META-INF/container.xml")
        if (meta != null) {
            val metaDoc = zip.getInputStream(meta).use { Jsoup.parse(it, null, "") }
            val path = metaDoc.getElementsByTag("rootfile").first()?.attr("full-path")
            if (path != null) {
                return path
            }
        }
        return "OEBPS/content.opf"
    }

    /**
     * Returns the package document where all the files are listed.
     */
    private fun getPackageDocument(zip: ZipFile, ref: String): Document {
        val entry = zip.getEntry(ref)
        return zip.getInputStream(entry).use { Jsoup.parse(it, null, "") }
    }

    /**
     * Returns all the pages from the epub.
     */
    private fun getPagesFromDocument(document: Document): List<String> {
        val pages = document.select("manifest > item")
            .filter { "application/xhtml+xml" == it.attr("media-type") }
            .associateBy { it.attr("id") }

        val spine = document.select("spine > itemref").map { it.attr("idref") }
        return spine.mapNotNull { pages[it] }.map { it.attr("href") }
    }

    /**
     * Returns all the images contained in every page from the epub.
     */
    private fun getImagesFromPages(zip: ZipFile, pages: List<String>, hrefs: Map<String, String>): List<String> {
        return pages.map { page ->
            val entry = zip.getEntry(hrefs[page])
            val document = zip.getInputStream(entry).use { Jsoup.parse(it, null, "") }
            document.getElementsByTag("img").mapNotNull { hrefs[it.attr("src")] }
        }.flatten()
    }

    /**
     * Returns a map with a relative url as key and abolute url as path.
     */
    private fun getHrefMap(packageHref: String, entries: List<String>): Map<String, String> {
        val lastSlashPos = packageHref.lastIndexOf('/')
        if (lastSlashPos < 0) {
            return entries.associateBy { it }
        }
        return entries.associateBy { entry ->
            if (entry.isNotBlank() && entry.length > lastSlashPos) {
                entry.substring(lastSlashPos + 1)
            } else {
                entry
            }
        }
    }

}
