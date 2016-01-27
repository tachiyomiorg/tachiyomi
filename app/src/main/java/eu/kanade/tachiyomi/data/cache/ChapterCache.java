package eu.kanade.tachiyomi.data.cache;

import android.content.Context;
import android.text.format.Formatter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jakewharton.disklrucache.DiskLruCache;
import com.squareup.okhttp.Response;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.List;

import eu.kanade.tachiyomi.data.source.model.Page;
import eu.kanade.tachiyomi.util.DiskUtils;
import okio.BufferedSink;
import okio.Okio;
import rx.Observable;

public class ChapterCache {

    private static final String PARAMETER_CACHE_DIRECTORY = "chapter_disk_cache";
    private static final int PARAMETER_APP_VERSION = 1;
    private static final int PARAMETER_VALUE_COUNT = 1;
    private static final int PARAMETER_CACHE_SIZE = 75 * 1024 * 1024;

    private Context context;
    private Gson gson;

    private DiskLruCache diskCache;

    public ChapterCache(Context context) {
        this.context = context;
        gson = new Gson();

        try {
            diskCache = DiskLruCache.open(
                    new File(context.getCacheDir(), PARAMETER_CACHE_DIRECTORY),
                    PARAMETER_APP_VERSION,
                    PARAMETER_VALUE_COUNT,
                    PARAMETER_CACHE_SIZE
            );
        } catch (IOException e) {
            // Do Nothing.
        }
    }

    public boolean remove(String file) {
        if (file.equals("journal") || file.startsWith("journal."))
            return false;

        try {
            String key = file.substring(0, file.lastIndexOf("."));
            return diskCache.remove(key);
        } catch (IOException e) {
            return false;
        }
    }

    public File getCacheDir() {
        return diskCache.getDirectory();
    }

    public long getRealSize() {
        return DiskUtils.getDirectorySize(getCacheDir());
    }

    public String getReadableSize() {
        return Formatter.formatFileSize(context, getRealSize());
    }

    public void setSize(int value) {
        diskCache.setMaxSize(value * 1024 * 1024);
    }

    public Observable<List<Page>> getPageUrlsFromDiskCache(final String chapterUrl) {
        return Observable.create(subscriber -> {
            try {
                List<Page> pages = getPageUrlsFromDiskCacheImpl(chapterUrl);
                subscriber.onNext(pages);
                subscriber.onCompleted();
            } catch (Throwable e) {
                subscriber.onError(e);
            }
        });
    }

    private List<Page> getPageUrlsFromDiskCacheImpl(String chapterUrl) throws IOException {
        DiskLruCache.Snapshot snapshot = null;
        List<Page> pages = null;

        try {
            String key = DiskUtils.hashKeyForDisk(chapterUrl);
            snapshot = diskCache.get(key);

            Type collectionType = new TypeToken<List<Page>>() {}.getType();
            pages = gson.fromJson(snapshot.getString(0), collectionType);
        } catch (IOException e) {
            // Do Nothing.
        } finally {
            if (snapshot != null) {
                snapshot.close();
            }
        }
        return pages;
    }

    public void putPageUrlsToDiskCache(final String chapterUrl, final List<Page> pages) {
        String cachedValue = gson.toJson(pages);

        DiskLruCache.Editor editor = null;
        OutputStream outputStream = null;
        try {
            String key = DiskUtils.hashKeyForDisk(chapterUrl);
            editor = diskCache.edit(key);
            if (editor == null) {
                return;
            }

            outputStream = new BufferedOutputStream(editor.newOutputStream(0));
            outputStream.write(cachedValue.getBytes());
            outputStream.flush();

            diskCache.flush();
            editor.commit();
        } catch (Exception e) {
            // Do Nothing.
        } finally {
            if (editor != null) {
                editor.abortUnlessCommitted();
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ignore) {
                    // Do Nothing.
                }
            }
        }
    }

    public boolean isImageInCache(final String imageUrl) {
        try {
            return diskCache.get(DiskUtils.hashKeyForDisk(imageUrl)) != null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public String getImagePath(final String imageUrl) {
        try {
            String imageName = DiskUtils.hashKeyForDisk(imageUrl) + ".0";
            File file = new File(diskCache.getDirectory(), imageName);
            return file.getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void putImageToDiskCache(final String imageUrl, final Response response) throws IOException {
        DiskLruCache.Editor editor = null;
        BufferedSink sink = null;

        try {
            String key = DiskUtils.hashKeyForDisk(imageUrl);
            editor = diskCache.edit(key);
            if (editor == null) {
                throw new IOException("Unable to edit key");
            }

            OutputStream outputStream = new BufferedOutputStream(editor.newOutputStream(0));
            sink = Okio.buffer(Okio.sink(outputStream));
            sink.writeAll(response.body().source());

            diskCache.flush();
            editor.commit();
        } catch (Exception e) {
            throw new IOException("Unable to save image");
        } finally {
            if (editor != null) {
                editor.abortUnlessCommitted();
            }
            if (sink != null) {
                sink.close();
            }
        }

    }

}

