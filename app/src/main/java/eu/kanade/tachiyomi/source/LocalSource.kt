package eu.kanade.tachiyomi.source

import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.model.*
import eu.kanade.tachiyomi.util.ChapterRecognition
import eu.kanade.tachiyomi.util.DiskUtil
import eu.kanade.tachiyomi.util.ZipContentProvider
import net.greypanther.natsort.CaseInsensitiveSimpleNaturalComparator
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
        private val FILE_PROTOCOL = "file://"
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
            val c = File.separator + context.getString(R.string.app_name) + File.separator + "local"
            return DiskUtil.getExternalStorages(context).map { File(it.absolutePath + c) }
        }
    }

    override val id = ID
    override val name = "LocalSource"
    override val lang = "en"
    override val supportsLatest = true

    override fun toString() = context.getString(R.string.local_source)

    override fun fetchMangaDetails(manga: SManga) = Observable.just(manga)

    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> {
        val chapters = getBaseDirectories(context)
                .mapNotNull { File(it, manga.url).listFiles()?.toList() }
                .flatten()
                .filter { it.isDirectory || isSupportedFormat(it.extension) }
                .map { chapterFile ->
                    SChapter.create().apply {
                        url = chapterFile.absolutePath
                        val chapName = if (chapterFile.isDirectory) {
                            chapterFile.name
                        } else {
                            chapterFile.nameWithoutExtension
                        }
                        val chapNameCut = chapName.replace(manga.title, "", true)
                        name = if (chapNameCut.isEmpty()) chapName else chapNameCut
                        date_upload = chapterFile.lastModified()
                        ChapterRecognition.parseChapterNumber(this, manga)
                    }
                }

        return Observable.just(chapters.sortedByDescending { it.chapter_number })
    }

    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
        val chapFile = File(chapter.url)
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
                        thumbnail_url = FILE_PROTOCOL + cover.absolutePath
                        break
                    }
                }

                // Copy the cover from the first chapter found.
                if (thumbnail_url == null) {
                    val chapters = fetchChapterList(this).toBlocking().first()
                    if (chapters.isNotEmpty()) {
                        val url = fetchPageList(chapters.last()).toBlocking().first().firstOrNull()?.url
                        if (url != null) {
                            val input = context.contentResolver.openInputStream(Uri.parse(url))
                            try {
                                val dest = updateCover(context, this, input)
                                thumbnail_url = dest?.let { FILE_PROTOCOL + it.absolutePath }
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

    private fun isSupportedFormat(extension: String): Boolean {
        return extension.equals("zip", true) || extension.equals("cbz", true)
    }

    private class OrderBy : Filter.Sort("Order by", arrayOf("Title", "Date"), Filter.Sort.Selection(0, true))

    override fun getFilterList() = FilterList(OrderBy())
}