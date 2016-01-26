package eu.kanade.tachiyomi.ui.download;

import android.os.Bundle;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import eu.kanade.tachiyomi.data.download.DownloadManager;
import eu.kanade.tachiyomi.data.download.model.Download;
import eu.kanade.tachiyomi.data.download.model.DownloadQueue;
import eu.kanade.tachiyomi.data.source.model.Page;
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class DownloadPresenter extends BasePresenter<DownloadFragment> {

    @Inject DownloadManager downloadManager;

    private DownloadQueue downloadQueue;
    private Subscription statusSubscription;
    private Subscription pageProgressSubscription;
    private HashMap<Download, Subscription> progressSubscriptions;

    public final static int GET_DOWNLOAD_QUEUE = 1;

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        downloadQueue = downloadManager.getQueue();
        progressSubscriptions = new HashMap<>();

        restartableLatestCache(GET_DOWNLOAD_QUEUE,
                () -> Observable.just(downloadQueue),
                DownloadFragment::onNextDownloads,
                (view, error) -> Timber.e(error.getMessage()));

        if (savedState == null)
            start(GET_DOWNLOAD_QUEUE);
    }

    @Override
    protected void onTakeView(DownloadFragment view) {
        super.onTakeView(view);

        add(statusSubscription = downloadQueue.getStatusObservable()
                .startWith(downloadQueue.getActiveDownloads())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(download -> {
                    processStatus(download, view);
                }));

        add(pageProgressSubscription = downloadQueue.getProgressObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(view::updateDownloadedPages));
    }

    @Override
    protected void onDropView() {
        destroySubscriptions();
        super.onDropView();
    }

    private void processStatus(Download download, DownloadFragment view) {
        switch (download.getStatus()) {
            case Download.DOWNLOADING:
                observeProgress(download, view);
                // Initial update of the downloaded pages
                view.updateDownloadedPages(download);
                break;
            case Download.DOWNLOADED:
                unsubscribeProgress(download);
                view.updateProgress(download);
                view.updateDownloadedPages(download);
                break;
            case Download.ERROR:
                unsubscribeProgress(download);
                break;
        }
    }

    private void observeProgress(Download download, DownloadFragment view) {
        Subscription subscription = Observable.interval(50, TimeUnit.MILLISECONDS, Schedulers.newThread())
                .flatMap(tick -> Observable.from(download.pages)
                        .map(Page::getProgress)
                        .reduce((x, y) -> x + y))
                .onBackpressureLatest()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(progress -> {
                    if (download.totalProgress != progress) {
                        download.totalProgress = progress;
                        view.updateProgress(download);
                    }
                });

        // Avoid leaking subscriptions
        Subscription oldSubscription = progressSubscriptions.remove(download);
        if (oldSubscription != null) oldSubscription.unsubscribe();

        progressSubscriptions.put(download, subscription);
    }

    private void unsubscribeProgress(Download download) {
        Subscription subscription = progressSubscriptions.remove(download);
        if (subscription != null)
            subscription.unsubscribe();
    }

    private void destroySubscriptions() {
        for (Subscription subscription : progressSubscriptions.values()) {
            subscription.unsubscribe();
        }
        progressSubscriptions.clear();

        remove(pageProgressSubscription);
        remove(statusSubscription);
    }

}
