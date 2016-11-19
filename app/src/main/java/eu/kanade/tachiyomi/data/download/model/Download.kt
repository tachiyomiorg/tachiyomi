package eu.kanade.tachiyomi.data.download.model

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.source.model.Page
import eu.kanade.tachiyomi.data.source.online.OnlineSource
import rx.subjects.PublishSubject

class Download(val source: OnlineSource, val manga: Manga, val chapter: Chapter) {

    var pages: List<Page>? = null

    @Volatile @Transient var totalProgress: Int = 0

    @Volatile @Transient var downloadedImages: Int = 0

    @Volatile @Transient var status: Int = 0
        set(status) {
            field = status
            statusSubject?.onNext(this)
        }

    @Transient private var statusSubject: PublishSubject<Download>? = null

    fun setStatusSubject(subject: PublishSubject<Download>?) {
        statusSubject = subject
    }

    companion object {

        const val NOT_DOWNLOADED = 0
        const val QUEUE = 1
        const val DOWNLOADING = 2
        const val DOWNLOADED = 3
        const val ERROR = 4
    }
}