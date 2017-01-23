package eu.kanade.tachiyomi.source

import android.content.Context
import android.net.Uri
import android.os.Environment
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.model.*
import net.greypanther.natsort.CaseInsensitiveSimpleNaturalComparator
import rx.Observable
import java.util.*
import java.util.regex.Pattern
import java.util.zip.ZipInputStream
import java.io.*

class LocalSource(private val context: Context) : CatalogueSource {
    private class OrderBy() : Filter.Sort("Order by", arrayOf("Title", "Date"), Filter.Sort.Selection(1, false))

    private val fileProtocol = "file:/"
    private val floatPattern = Pattern.compile("[0-9]+[.]?[0-9]*")
    private fun isImage(name: String) = name.endsWith(".jpg", true) || name.endsWith(".jpeg", true) || name.endsWith(".png", true) || name.endsWith(".gif", true)

    override val id = 0L;
    override val name = "LocalSource"
    override val lang = "en"
    override val supportsLatest = false

    override fun toString() = context.getString(R.string.local_source)

    override fun fetchMangaDetails(manga: SManga) = Observable.from(arrayOf(manga))

    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> {
        val chapters = File(manga.url.substring((6))).listFiles()
                .filter { it.isDirectory || it.extension.equals("zip", true) || it.extension.equals("cbz", true) }
                .map { chapterFile ->
                    SChapter.create().apply {
                        url = fileProtocol + chapterFile.absolutePath
                        val chapName = if (chapterFile.isDirectory) chapterFile.name else chapterFile.nameWithoutExtension
                        val chapNameCut = chapName.replace(manga.title, "", true)
                        name = if (chapNameCut.isEmpty()) chapName else chapNameCut
                        date_upload = chapterFile.lastModified()
                        var chapNum = "0"
                        val matcher = floatPattern.matcher(name)
                        while (matcher.find())
                            chapNum = matcher.group()
                        chapter_number = chapNum.toFloat()
                    }
                }
        return Observable.from(arrayOf(chapters.sortedByDescending { it.chapter_number }))
    }

    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
        val chapFile = File(chapter.url.substring(6))
        val pages: List<Page>
        if (chapFile.isDirectory) {
            pages = chapFile.listFiles()
                    .filter { !it.isDirectory && isImage(it.name) }
                    .sortedWith(Comparator<File> { t1, t2 -> CaseInsensitiveSimpleNaturalComparator.getInstance<String>().compare(t1.nameWithoutExtension, t2.nameWithoutExtension) })
                    .withIndex().mapIndexed { i, v -> Page(i, v.value.absolutePath, v.value.absolutePath, Uri.fromFile(v.value)).apply { status = Page.READY } }
        } else {
            val destDir = File(context.cacheDir.absolutePath + File.separator + chapFile.path.replace(File.separator, "_"))
            if (!destDir.exists())
                destDir.mkdir()

            val images = ArrayList<Pair<String, Uri>>()
            val zipIn = ZipInputStream(FileInputStream(chapFile))
            var entry = zipIn.nextEntry
            while (entry != null) {
                val filePath = destDir.absolutePath + File.separator + entry.name
                val file = File(filePath)
                if (entry.isDirectory) {
                    if (!file.exists())
                        file.mkdir()
                } else if (isImage(entry.name)) {
                    images.add(Pair(entry.name, Uri.fromFile(file)))
                    if (!file.exists()) {
                        val bos = BufferedOutputStream(FileOutputStream(filePath))
                        val bytesIn = ByteArray(4096)
                        var read: Int
                        while (true) {
                            read = zipIn.read(bytesIn)
                            if (read == -1) break;
                            bos.write(bytesIn, 0, read)
                        }
                        bos.close()
                    }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
            zipIn.close()
            pages = images.sortedWith(Comparator<Pair<String, Uri>> { t1, t2 -> CaseInsensitiveSimpleNaturalComparator.getInstance<String>().compare(t1.first, t2.first) })
                    .mapIndexed { i, v -> Page(i, chapter.url + "!" + v.first, chapter.url + "!" + v.first, v.second).apply { status = Page.READY } }
        }
        return Observable.from(arrayOf(pages))
    }

    override fun fetchPopularManga(page: Int) = fetchSearchManga(page, "", getFilterList())

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> {
        val mangaBaseDir = File(Environment.getExternalStorageDirectory().absolutePath +
                File.separator + context.getString(R.string.app_name), "mangas")
        var mangaDirs = mangaBaseDir.listFiles()
                .filter { it.isDirectory && it.name.contains(query, true) }
        if (!filters.isEmpty()) {
            val state = (filters.get(0) as OrderBy).state
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
        }
        val mangas = mangaDirs.map { mangaDir ->
            SManga.create().apply {
                title = mangaDir.name
                url = fileProtocol + mangaDir.absolutePath
                val coverPath = mangaDir.absolutePath + File.separator + "cover.jpg"
                if (File(coverPath).exists()) thumbnail_url = fileProtocol + coverPath
                initialized = true
            }
        }
        return Observable.from(arrayOf(MangasPage(mangas, false)))
    }

    override fun fetchLatestUpdates(page: Int): Observable<MangasPage> {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getFilterList() = FilterList(OrderBy())
}