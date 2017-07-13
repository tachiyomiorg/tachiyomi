package eu.kanade.tachiyomi.source

import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.model.*
import eu.kanade.tachiyomi.util.ChapterRecognition
import eu.kanade.tachiyomi.util.DiskUtil
import eu.kanade.tachiyomi.util.RarContentProvider
import eu.kanade.tachiyomi.util.ZipContentProvider
import junrar.Archive
import junrar.rarfile.FileHeader
import net.greypanther.natsort.CaseInsensitiveSimpleNaturalComparator
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import rx.Observable
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class LocalSource(private val context: Context) : CatalogueSource {
    companion object {
        private val COVER_NAME = "cover.jpg"
        private val POPULAR_FILTERS = FilterList(OrderBy())
        private val LATEST_FILTERS = FilterList(OrderBy().apply { state = Filter.Sort.Selection(1, false) })
        private val LATEST_THRESHOLD = TimeUnit.MILLISECONDS.convert(7, TimeUnit.DAYS)
        val ID = 0L

        fun updateCover(context: Context, manga: SManga, input: InputStream): File? {
            val dir = getBaseDirectories(context).firstOrNull()
            if (dir == null) {
                input.close()
                return null
            }
            val cover = File("${dir.absolutePath}/${manga.url}", COVER_NAME)

            // It might not exist if using the external SD card
            cover.parentFile.mkdirs()
            input.use {
                cover.outputStream().use {
                    input.copyTo(it)
                }
            }
            return cover
        }

        private fun getBaseDirectories(context: Context): List<File> {
            val c = context.getString(R.string.app_name) + File.separator + "local"
            return DiskUtil.getExternalStorages(context).map { File(it.absolutePath, c) }
        }
    }

    override val id = ID
    override val name = "LocalSource"
    override val lang = ""
    override val supportsLatest = true

    override fun toString() = context.getString(R.string.local_source)

    override fun fetchPopularManga(page: Int) = fetchSearchManga(page, "", POPULAR_FILTERS)

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> {
        val baseDirs = getBaseDirectories(context)

        val time = if (filters === LATEST_FILTERS) System.currentTimeMillis() - LATEST_THRESHOLD else 0L
        var mangaDirs = baseDirs.mapNotNull { it.listFiles()?.toList() }
                .flatten()
                .filter { it.isDirectory && if (time == 0L) it.name.contains(query, ignoreCase = true) else it.lastModified() >= time }
                .distinctBy { it.name }

        val state = ((if (filters.isEmpty()) POPULAR_FILTERS else filters)[0] as OrderBy).state
        when (state?.index) {
            0 -> {
                if (state!!.ascending)
                    mangaDirs = mangaDirs.sortedBy { it.name.toLowerCase(Locale.ENGLISH) }
                else
                    mangaDirs = mangaDirs.sortedByDescending { it.name.toLowerCase(Locale.ENGLISH) }
            }
            1 -> {
                if (state!!.ascending)
                    mangaDirs = mangaDirs.sortedBy(File::lastModified)
                else
                    mangaDirs = mangaDirs.sortedByDescending(File::lastModified)
            }
        }

        val mangas = mangaDirs.map { mangaDir ->
            SManga.create().apply {
                title = mangaDir.name
                url = mangaDir.name

                // Try to find the cover
                for (dir in baseDirs) {
                    val cover = File("${dir.absolutePath}/$url", COVER_NAME)
                    if (cover.exists()) {
                        thumbnail_url = cover.absolutePath
                        break
                    }
                }

                // Copy the cover from the first chapter found.
                if (thumbnail_url == null) {
                    val chapters = fetchChapterList(this).toBlocking().first()
                    if (chapters.isNotEmpty()) {
                        val uri = fetchPageList(chapters.last()).toBlocking().first().firstOrNull()?.uri
                        if (uri != null) {
                            val input = context.contentResolver.openInputStream(uri)
                            try {
                                val dest = updateCover(context, this, input)
                                thumbnail_url = dest?.absolutePath
                            } catch (e: Exception) {
                                Timber.e(e)
                            }
                        }
                    }
                }

                initialized = true
            }
        }
        return Observable.just(MangasPage(mangas, false))
    }

    override fun fetchLatestUpdates(page: Int) = fetchSearchManga(page, "", LATEST_FILTERS)

    override fun fetchMangaDetails(manga: SManga) = Observable.just(manga)

    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> {
        val comparator = CaseInsensitiveSimpleNaturalComparator.getInstance<String>()
        val chapters = getBaseDirectories(context)
                .mapNotNull { File(it, manga.url).listFiles()?.toList() }
                .flatten()
                .filter { it.isDirectory || isSupportedFormat(it.extension) }
                .map { chapterFile ->
                    SChapter.create().apply {
                        url = "${manga.url}/${chapterFile.name}"
                        val chapName = if (chapterFile.isDirectory) {
                            chapterFile.name
                        } else {
                            chapterFile.nameWithoutExtension
                        }
                        val chapNameCut = chapName.replace(manga.title, "", true).trim()
                        name = if (chapNameCut.isEmpty()) chapName else chapNameCut
                        date_upload = chapterFile.lastModified()
                        ChapterRecognition.parseChapterNumber(this, manga)
                    }
                }
                .sortedWith(Comparator<SChapter> { c1, c2 ->
                    val c = c2.chapter_number.compareTo(c1.chapter_number)
                    if (c == 0) comparator.compare(c2.name, c1.name) else c
                })

        return Observable.just(chapters)
    }

    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
        val baseDirs = getBaseDirectories(context)

        for (dir in baseDirs) {
            val chapFile = File(dir, chapter.url)
            if (!chapFile.exists()) continue

            return Observable.just(getLoader(chapFile).load())
        }

        return Observable.error(Exception("Chapter not found"))
    }

    private fun isSupportedFormat(extension: String): Boolean {
        return extension.equals("zip", true) || extension.equals("cbz", true)
                || extension.equals("rar", true) || extension.equals("cbr", true)
                || extension.equals("epub", true)
    }

    private fun getLoader(file: File): Loader {
        val extension = file.extension
        return if (file.isDirectory) {
            DirectoryLoader(file)
        } else if (extension.equals("zip", true) || extension.equals("cbz", true)) {
            ZipLoader(file)
        } else if (extension.equals("epub", true)) {
            EpubLoader(file)
        } else if (extension.equals("rar", true) || extension.equals("cbr", true)) {
            RarLoader(file)
        } else {
            throw Exception("Invalid chapter format")
        }
    }

    private class OrderBy : Filter.Sort("Order by", arrayOf("Title", "Date"), Filter.Sort.Selection(0, true))

    override fun getFilterList() = FilterList(OrderBy())

    interface Loader {
        fun load(): List<Page>
    }

    class DirectoryLoader(val file: File) : Loader {
        override fun load(): List<Page> {
            val comparator = CaseInsensitiveSimpleNaturalComparator.getInstance<String>()
            return file.listFiles()
                    .filter { !it.isDirectory && DiskUtil.isImage(it.name, { FileInputStream(it) }) }
                    .sortedWith(Comparator<File> { f1, f2 -> comparator.compare(f1.name, f2.name) })
                    .map { Uri.fromFile(it) }
                    .mapIndexed { i, uri -> Page(i, uri = uri).apply { status = Page.READY } }
        }
    }

    class ZipLoader(val file: File) : Loader {
        override fun load(): List<Page> {
            val comparator = CaseInsensitiveSimpleNaturalComparator.getInstance<String>()
            return ZipFile(file).use { zip ->
                zip.entries().toList()
                        .filter { !it.isDirectory && DiskUtil.isImage(it.name, { zip.getInputStream(it) }) }
                        .sortedWith(Comparator<ZipEntry> { f1, f2 -> comparator.compare(f1.name, f2.name) })
                        .map { Uri.parse("content://${ZipContentProvider.PROVIDER}${file.absolutePath}!/${it.name}") }
                        .mapIndexed { i, uri -> Page(i, uri = uri).apply { status = Page.READY } }
            }
        }
    }

    class RarLoader(val file: File) : Loader {
        override fun load(): List<Page> {
            val comparator = CaseInsensitiveSimpleNaturalComparator.getInstance<String>()
            return Archive(file).use { archive ->
                archive.fileHeaders
                        .filter { !it.isDirectory && DiskUtil.isImage(it.fileNameString, { archive.getInputStream(it) }) }
                        .sortedWith(Comparator<FileHeader> { f1, f2 -> comparator.compare(f1.fileNameString, f2.fileNameString) })
                        .map { Uri.parse("content://${RarContentProvider.PROVIDER}${file.absolutePath}!-/${it.fileNameString}") }
                        .mapIndexed { i, uri -> Page(i, uri = uri).apply { status = Page.READY } }
            }
        }
    }

    class EpubLoader(val file: File) : Loader {

        override fun load(): List<Page> {
            ZipFile(file).use { zip ->
                val allEntries = zip.entries().toList()
                val ref = getPackageHref(zip)
                val doc = getPackageDocument(zip, ref)
                val pages = getPagesFromDocument(doc)
                val hrefs = getHrefMap(ref, allEntries.map { it.name })
                return getImagesFromPages(zip, pages, hrefs)
                        .map { Uri.parse("content://${ZipContentProvider.PROVIDER}${file.absolutePath}!/$it") }
                        .mapIndexed { i, uri -> Page(i, uri = uri).apply { status = Page.READY } }
            }
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
}