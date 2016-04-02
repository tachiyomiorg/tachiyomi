package eu.kanade.tachiyomi.data.download.model;

import java.io.File;
import java.util.List;

import eu.kanade.tachiyomi.data.database.models.Chapter;
import eu.kanade.tachiyomi.data.database.models.Manga;
import eu.kanade.tachiyomi.data.source.base.OnlineSource;
import eu.kanade.tachiyomi.data.source.model.Page;
import rx.subjects.PublishSubject;

public class Download {
    public OnlineSource source;
    public Manga manga;
    public Chapter chapter;
    public List<Page> pages;
    public File directory;

    public transient volatile int totalProgress;
    public transient volatile int downloadedImages;
    private transient volatile int status;

    private transient PublishSubject<Download> statusSubject;

    public static final int NOT_DOWNLOADED = 0;
    public static final int QUEUE = 1;
    public static final int DOWNLOADING = 2;
    public static final int DOWNLOADED = 3;
    public static final int ERROR = 4;


    public Download(OnlineSource source, Manga manga, Chapter chapter) {
        this.source = source;
        this.manga = manga;
        this.chapter = chapter;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
        notifyStatus();
    }

    public void setStatusSubject(PublishSubject<Download> subject) {
        this.statusSubject = subject;
    }

    private void notifyStatus() {
        if (statusSubject != null)
            statusSubject.onNext(this);
    }
}