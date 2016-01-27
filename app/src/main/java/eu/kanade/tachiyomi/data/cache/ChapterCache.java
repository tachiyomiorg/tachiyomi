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

/**
 * Class used to create chapter cache
 * For each image in a chapter a file is created
 * For each chapter a Json list is created and converted to a file.
 * The files are in format *md5key*.0
 */
public class ChapterCache {

    /** Name of cache directory. */
    private static final String PARAMETER_CACHE_DIRECTORY = "chapter_disk_cache";

    /** Application cache version. */
    private static final int PARAMETER_APP_VERSION = 1;

    /** The number of values per cache entry. Must be positive. */
    private static final int PARAMETER_VALUE_COUNT = 1;

    /** The maximum number of bytes this cache should use to store. */
    private static final int PARAMETER_CACHE_SIZE = 75 * 1024 * 1024;

    /** Interface to global information about an application environment. */
    private final Context context;

    /** Google Json class used for parsing json files. */
    private final Gson gson;

    /** Cache class used for cache management. */
    private DiskLruCache diskCache;

    /**
     * Constructor of ChapterCache.
     * @param context application environment interface.
     */
    public ChapterCache(Context context) {
        this.context = context;

        // Initialize Json handler.
        gson = new Gson();

        // Try to open cache in default cache directory.
        try {
            diskCache = DiskLruCache.open(
                    new File(context.getCacheDir(), PARAMETER_CACHE_DIRECTORY),
                    PARAMETER_APP_VERSION,
                    PARAMETER_VALUE_COUNT,
                    PARAMETER_CACHE_SIZE
            );
        } catch (IOException e) {
            // Do Nothing. TODO error handling.
        }
    }

    /**
     * Remove file from cache.
     * @param file name of chapter file md5.0.
     * @return false if file is journal or error else returns status of deletion.
     */
    public boolean removeFileFromCache(String file) {
        // Make sure we don't delete the journal file (keeps track of cache).
        if (file.equals("journal") || file.startsWith("journal."))
            return false;

        try {
            // Take dot(.) substring to get filename without the .0 at the end.
            String key = file.substring(0, file.lastIndexOf("."));
            // Remove file from cache.
            return diskCache.remove(key);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Returns directory of cache.
     * @return directory of cache.
     */
    public File getCacheDir() {
        return diskCache.getDirectory();
    }

    /**
     * Returns real size of directory.
     * @return real size of directory.
     */
    private long getRealSize() {
        return DiskUtils.getDirectorySize(getCacheDir());
    }

    /**
     * Returns real size of directory in human readable format.
     * @return real size of directory.
     */
    public String getReadableSize() {
        return Formatter.formatFileSize(context, getRealSize());
    }

    /**
     * Get page objects from cache.
     * @param chapterUrl the url of the chapter.
     * @return list of chapter pages.
     */
    public Observable<List<Page>> getPageUrlsFromDiskCache(final String chapterUrl) {
        return Observable.create(subscriber -> {
            try {
                // Get list of pages from chapterUrl.
                List<Page> pages = getPageUrlsFromDiskCacheImpl(chapterUrl);
                // Provides the Observer with a new item to observe.
                subscriber.onNext(pages);
                // Notify the Observer that finished sending push-based notifications.
                subscriber.onCompleted();
            } catch (Throwable e) {
                subscriber.onError(e);
            }
        });
    }

    /**
     * Implementation of the getPageUrlsFromDiskCache() function
     * @param chapterUrl the url of the chapter
     * @return returns list of chapter pages
     * @throws IOException does nothing atm
     */
    private List<Page> getPageUrlsFromDiskCacheImpl(String chapterUrl) throws IOException /*TODO IOException never thrown*/ {
        // Initialize snapshot (a snapshot of the values for an entry).
        DiskLruCache.Snapshot snapshot = null;

        // Initialize list of pages.
        List<Page> pages = null;

        try {
            // Create md5 key and retrieve snapshot.
            String key = DiskUtils.hashKeyForDisk(chapterUrl);
            snapshot = diskCache.get(key);


            // Convert JSON string to list of objects.
            Type collectionType = new TypeToken<List<Page>>() {}.getType();
            pages = gson.fromJson(snapshot.getString(0), collectionType);

        } catch (IOException e) {
            // Do Nothing. //TODO error handling?
        } finally {
            if (snapshot != null) {
                snapshot.close();
            }
        }
        return pages;
    }

    /**
     * Add page urls to disk cache.
     * @param chapterUrl the url of the chapter.
     * @param pages list of chapter pages.
     */
    public void putPageUrlsToDiskCache(final String chapterUrl, final List<Page> pages) {
        // Convert list of pages to json string.
        String cachedValue = gson.toJson(pages);

        // Initialize the editor (edits the values for an entry).
        DiskLruCache.Editor editor = null;

        // Initialize OutputStream.
        OutputStream outputStream = null;

        try {
            // Get editor from md5 key.
            String key = DiskUtils.hashKeyForDisk(chapterUrl);
            editor = diskCache.edit(key);
            if (editor == null) {
                return;
            }

            // Write chapter urls to cache.
            outputStream = new BufferedOutputStream(editor.newOutputStream(0));
            outputStream.write(cachedValue.getBytes());
            outputStream.flush();

            diskCache.flush();
            editor.commit();
        } catch (Exception e) {
            // Do Nothing. TODO error handling?
        } finally {
            if (editor != null) {
                editor.abortUnlessCommitted();
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ignore) {
                    // Do Nothing. TODO error handling?
                }
            }
        }
    }

    /**
     * Check if image is in cache.
     * @param imageUrl url of image.
     * @return true if in cache otherwise false.
     */
    public boolean isImageInCache(final String imageUrl) {
        try {
            return diskCache.get(DiskUtils.hashKeyForDisk(imageUrl)) != null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Get image path from url.
     * @param imageUrl url of image.
     * @return path of image.
     */
    public String getImagePath(final String imageUrl) {
        try {
            // Get file from md5 key.
            String imageName = DiskUtils.hashKeyForDisk(imageUrl) + ".0";
            File file = new File(diskCache.getDirectory(), imageName);
            return file.getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Add image to cache
     * @param imageUrl url of image.
     * @param response http response from page.
     * @throws IOException image error.
     */
    public void putImageToDiskCache(final String imageUrl, final Response response) throws IOException {
        // Initialize editor (edits the values for an entry).
        DiskLruCache.Editor editor = null;

        // Initialize BufferedSink (used for small writes).
        BufferedSink sink = null;

        try {
            // Get editor from md5 key.
            String key = DiskUtils.hashKeyForDisk(imageUrl);
            editor = diskCache.edit(key);
            if (editor == null) {
                throw new IOException("Unable to edit key");
            }

            // Initialize OutputStream and write image.
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

