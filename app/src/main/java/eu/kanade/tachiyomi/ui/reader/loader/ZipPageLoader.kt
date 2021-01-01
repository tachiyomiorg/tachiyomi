package eu.kanade.tachiyomi.ui.reader.loader

import android.os.Build
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.util.lang.compareToCaseInsensitiveNaturalOrder
import eu.kanade.tachiyomi.util.system.ImageUtil
import rx.Observable
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.zip.ZipFile

/**
 * Loader used to load a chapter from a .zip or .cbz file.
 */
class ZipPageLoader(file: File) : PageLoader() {

    /**
     * The zip file to load pages from.
     */
    private val zip = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        ZipFile(file, StandardCharsets.ISO_8859_1)
    } else {
        ZipFile(file)
    }

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
        return zip.entries().toList()
            .filter { !it.isDirectory && ImageUtil.isImage(it.name) { zip.getInputStream(it) } }
            .sortedWith { f1, f2 -> f1.name.compareToCaseInsensitiveNaturalOrder(f2.name) }
            .mapIndexed { i, entry ->
                val streamFn = { zip.getInputStream(entry) }
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
        return Observable.just(
            if (isRecycled) {
                Page.ERROR
            } else {
                Page.READY
            }
        )
    }
}
