package eu.kanade.tachiyomi.util

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Wrapper over ZipFile to load files in epub format.
 */
class EpubFile(file: File) : Closeable {

    /**
     * Zip file of this epub.
     */
    private val zip = ZipFile(file)

    /**
     * Closes the underlying zip file.
     */
    override fun close() {
        zip.close()
    }

    /**
     * Returns an input stream for reading the contents of the specified zip file entry.
     */
    fun getInputStream(entry: ZipEntry): InputStream {
        return zip.getInputStream(entry)
    }

    /**
     * Returns the zip file entry for the specified name, or null if not found.
     */
    fun getEntry(name: String): ZipEntry? {
        return zip.getEntry(name)
    }

    /**
     * Returns the path of all the images found in the epub file.
     */
    fun getImagesFromPages(): List<String> {
        val allEntries = zip.entries().toList()
        val ref = getPackageHref()
        val doc = getPackageDocument(ref)
        val pages = getPagesFromDocument(doc)
        val hrefs = getHrefMap(ref, allEntries.map { it.name })
        return getImagesFromPages(pages, hrefs)
    }

    /**
     * Returns the path to the package document.
     */
    private fun getPackageHref(): String {
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
    private fun getPackageDocument(ref: String): Document {
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
    private fun getImagesFromPages(pages: List<String>, hrefs: Map<String, String>): List<String> {
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
