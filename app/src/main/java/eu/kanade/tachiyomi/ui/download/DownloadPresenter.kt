package eu.kanade.tachiyomi.ui.download

import android.os.Bundle
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.download.model.DownloadQueue
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import rx.Observable
import timber.log.Timber
import javax.inject.Inject

/**
 * Presenter of [DownloadFragment].
 */
class DownloadPresenter : BasePresenter<DownloadFragment>() {

    companion object {
        /**
         * Id of the restartable that returns the download queue.
         */
        const val GET_DOWNLOAD_QUEUE = 1
    }

    /**
     * Download manager.
     */
    @Inject lateinit var downloadManager: DownloadManager

    /**
     * Property to get the queue from the download manager.
     */
    val downloadQueue: DownloadQueue
        get() = downloadManager.queue

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        restartableLatestCache(GET_DOWNLOAD_QUEUE,
                { Observable.just(downloadQueue) },
                { view, downloads -> view.onNextDownloads(downloads) },
                { view, error -> Timber.e(error.message) })

        if (savedState == null) {
            start(GET_DOWNLOAD_QUEUE)
        }
    }

    fun getStatusObservable(): Observable<Download> {
        return downloadQueue.getStatusObservable()
                .startWith(downloadQueue.getActiveDownloads())
    }

    fun getProgressObservable(): Observable<Download> {
        return downloadQueue.getProgressObservable()
                .onBackpressureBuffer()
    }

    /**
     * Clears the download queue.
     */
    fun clearQueue() {
        downloadManager.clearQueue()
        start(GET_DOWNLOAD_QUEUE)
    }

}
