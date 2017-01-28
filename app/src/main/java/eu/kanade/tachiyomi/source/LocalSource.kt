package eu.kanade.tachiyomi.source

import android.content.Context
import android.net.Uri
import android.os.Environment
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.model.*
import eu.kanade.tachiyomi.util.ChapterRecognition
import eu.kanade.tachiyomi.util.DiskUtil
import eu.kanade.tachiyomi.util.ZipContentProvider
import net.greypanther.natsort.CaseInsensitiveSimpleNaturalComparator
import rx.Observable
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class LocalSource(private val context: Context) : CatalogueSource {
    companion object {
        private val FILE_PROTOCOL = "file://"
        private val COVER_NAME = "cover.jpg"
        private val POPULAR_FILTERS = FilterList(OrderBy())
        private val LATEST_FILTERS = FilterList(OrderBy().apply { state = Filter.Sort.Selection(1, false) })
        private val LATEST_THRESHOLD = TimeUnit.MILLISECONDS.convert(7, TimeUnit.DAYS)
        val ID = 0L
        fun updateCover(manga: SManga, inputStream: InputStream) {
            val coverUrl = manga.url + File.separator + COVER_NAME
            File(coverUrl.substring(FILE_PROTOCOL.length)).outputStream().use {
                inputStream.copyTo(it)
                it.flush()
                manga.thumbnail_url = coverUrl
            }
        }

        fun updateCover(manga: SManga, url: String, context: Context) {
            (if (url.startsWith(FILE_PROTOCOL))
                File(url.substring(FILE_PROTOCOL.length)).inputStream()
            else
                context.contentResolver.openInputStream(Uri.parse(url))).use {
                updateCover(manga, it)
            }
        }
    }

    private class OrderBy() : Filter.Sort("Order by", arrayOf("Title", "Date"), Filter.Sort.Selection(0, true))

    override val id = ID
    override val name = "LocalSource"
    override val lang = "en"
    override val supportsLatest = true

    override fun toString() = context.getString(R.string.local_source)

    override fun fetchMangaDetails(manga: SManga) = Observable.just(manga)

    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> {
        val chapters = File(manga.url.substring((FILE_PROTOCOL.length))).listFiles()
                .filter { it.isDirectory || it.extension.equals("zip", true) || it.extension.equals("cbz", true) }
                .map { chapterFile ->
                    SChapter.create().apply {
                        url = FILE_PROTOCOL + chapterFile.absolutePath
                        val chapName = if (chapterFile.isDirectory) chapterFile.name else chapterFile.nameWithoutExtension
                        val chapNameCut = chapName.replace(manga.title, "", true)
                        name = if (chapNameCut.isEmpty()) chapName else chapNameCut
                        date_upload = chapterFile.lastModified()
                        ChapterRecognition.parseChapterNumber(this, manga)
                    }
                }
        return Observable.just(chapters.sortedByDescending { it.chapter_number })
    }

    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
        val chapFile = File(chapter.url.substring(FILE_PROTOCOL.length))
        if (chapFile.isDirectory) {
            return Observable.just(chapFile.listFiles()
                    .filter { !it.isDirectory && DiskUtil.isImage(it.name, { FileInputStream(it) }) }
                    .sortedWith(Comparator<File> { t1, t2 -> CaseInsensitiveSimpleNaturalComparator.getInstance<String>().compare(t1.name, t2.name) })
                    .mapIndexed { i, v -> Page(i, FILE_PROTOCOL + v.absolutePath, FILE_PROTOCOL + v.absolutePath, Uri.fromFile(v)).apply { status = Page.READY } })
        } else {
            val zip = ZipFile(chapFile)
            return Observable.just(ZipFile(chapFile).entries().toList()
                    .filter { !it.isDirectory && DiskUtil.isImage(it.name, { zip.getInputStream(it) }) }
                    .sortedWith(Comparator<ZipEntry> { t1, t2 -> CaseInsensitiveSimpleNaturalComparator.getInstance<String>().compare(t1.name, t2.name) })
                    .mapIndexed { i, v ->
                        val path = "content://${ZipContentProvider.PROVIDER}${chapFile.absolutePath}!/${v.name}"
                        Page(i, path, path, Uri.parse(path)).apply { status = Page.READY }
                    })
        }
    }

    override fun fetchPopularManga(page: Int) = fetchSearchManga(page, "", POPULAR_FILTERS)

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> {
        val mangaBaseDir = File(Environment.getExternalStorageDirectory().absolutePath +
                File.separator + context.getString(R.string.app_name), "local")

        val time = if (filters === LATEST_FILTERS) System.currentTimeMillis() - LATEST_THRESHOLD else 0L
        var mangaDirs = mangaBaseDir.listFiles()
                .filter { it.isDirectory && if (time == 0L) it.name.contains(query, true) else it.lastModified() >= time }
        val state = ((if (filters.isEmpty()) POPULAR_FILTERS else filters).get(0) as OrderBy).state
        when (state?.index) {
            0 -> {
                if (state!!.ascending)
                    mangaDirs = mangaDirs.sortedBy { it.name.toLowerCase(Locale.ENGLISH) }
                else
                    mangaDirs = mangaDirs.sortedByDescending { it.name.toLowerCase(Locale.ENGLISH) }
            }
            1 -> {
                if (state!!.ascending)
                    mangaDirs = mangaDirs.sortedBy { it.lastModified() }
                else
                    mangaDirs = mangaDirs.sortedByDescending { it.lastModified() }
            }
        }
        val mangas = mangaDirs.map { mangaDir ->
            SManga.create().apply {
                title = mangaDir.name
                url = FILE_PROTOCOL + mangaDir.absolutePath
                if (File(mangaDir, COVER_NAME).exists()) {
                    thumbnail_url = url + File.separator + COVER_NAME
                } else {
                    var p = url.substring(0)
                    p = p.substring(0)
                    val chapters = fetchChapterList(this).toBlocking().first()
                    if (chapters.isNotEmpty()) {
                        val url = fetchPageList(chapters.last()).toBlocking().first().firstOrNull()?.url
                        if (url != null) updateCover(this, url, context)
                    }
                }
                initialized = true
            }
        }
        return Observable.from(arrayOf(MangasPage(mangas, false)))
    }

    override fun fetchLatestUpdates(page: Int) = fetchSearchManga(page, "", LATEST_FILTERS)

    override fun getFilterList() = FilterList(OrderBy())
}