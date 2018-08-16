package eu.kanade.tachiyomi.ui.reader.loader

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.util.ImageUtil
import net.greypanther.natsort.CaseInsensitiveSimpleNaturalComparator
import rx.Observable
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class ZipPageLoader(file: File) : PageLoader() {

    private val zip = ZipFile(file)

    override fun recycle() {
        super.recycle()
        zip.close()
    }

    override fun getPages(): Observable<List<ReaderPage>> {
        val comparator = CaseInsensitiveSimpleNaturalComparator.getInstance<String>()

        return zip.entries().toList()
            .filter { !it.isDirectory && ImageUtil.isImage(it.name) { zip.getInputStream(it) } }
            .sortedWith(Comparator<ZipEntry> { f1, f2 -> comparator.compare(f1.name, f2.name) })
            .mapIndexed { i, entry ->
                val streamFn = { zip.getInputStream(entry) }
                ReaderPage(i).apply {
                    stream = streamFn
                    status = Page.READY
                }
            }
            .let { Observable.just(it) }
    }

    override fun getPage(page: ReaderPage): Observable<Int> {
        return Observable.just(if (isRecycled) {
            Page.ERROR
        } else {
            Page.READY
        })
    }
}
