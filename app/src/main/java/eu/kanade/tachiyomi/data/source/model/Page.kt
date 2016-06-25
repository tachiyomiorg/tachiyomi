package eu.kanade.tachiyomi.data.source.model

import eu.kanade.tachiyomi.data.network.ProgressListener
import eu.kanade.tachiyomi.ui.reader.ReaderChapter
import rx.subjects.Subject

class Page(
        val pageNumber: Int,
        val url: String,
        var imageUrl: String? = null,
        @Transient var imagePath: String? = null
) : ProgressListener {

    @Transient lateinit var chapter: ReaderChapter

    @Transient @Volatile var status: Int = 0
        set(value) {
            field = value
            statusSubject?.onNext(value)
        }

    @Transient @Volatile var progress: Int = 0

    @Transient private var statusSubject: Subject<Int, Int>? = null

    override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
        progress = (100 * bytesRead / contentLength).toInt()
    }

    fun setStatusSubject(subject: Subject<Int, Int>?) {
        this.statusSubject = subject
    }

    companion object {

        const val QUEUE = 0
        const val LOAD_PAGE = 1
        const val DOWNLOAD_IMAGE = 2
        const val READY = 3
        const val ERROR = 4
    }

}
