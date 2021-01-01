package eu.kanade.tachiyomi.ui.reader.loader

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.util.lang.compareToCaseInsensitiveNaturalOrder
import eu.kanade.tachiyomi.util.system.ImageUtil
import rx.Observable
import java.io.File
import java.io.FileInputStream

/**
 * Loader used to load a chapter from a directory given on [file].
 */
class DirectoryPageLoader(val file: File) : PageLoader() {

    /**
     * Returns an observable containing the pages found on this directory ordered with a natural
     * comparator.
     */
    override fun getPages(): Observable<List<ReaderPage>> {
        return file.listFiles()
            .filter { !it.isDirectory && ImageUtil.isImage(it.name) { FileInputStream(it) } }
            .sortedWith { f1, f2 -> f1.name.compareToCaseInsensitiveNaturalOrder(f2.name) }
            .mapIndexed { i, file ->
                val streamFn = { FileInputStream(file) }
                ReaderPage(i).apply {
                    stream = streamFn
                    status = Page.READY
                }
            }
            .let { Observable.just(it) }
    }

    /**
     * Returns an observable that emits a ready state.
     */
    override fun getPage(page: ReaderPage): Observable<Int> {
        return Observable.just(Page.READY)
    }
}
