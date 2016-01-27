package eu.kanade.tachiyomi.data.cache;

import android.content.Context;
import android.text.TextUtils;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import eu.kanade.tachiyomi.util.DiskUtils;

/**
 * Class used to create cover cache
 * Makes use of Glide(which can avoid repeating requests) for saving the file.
 * It is not necessary to load the images to the cache.
 * Names of files are created with the md5 of the thumbnailURL
 */
public class CoverCache {

    /**
     * Name of cache directory.
     */
    private static final String PARAMETER_CACHE_DIRECTORY = "cover_disk_cache";

    /**
     * Interface to global information about an application environment.
     */
    private final Context context;

    /**
     * Cache class used for cache management.
     */
    private final File cacheDir;

    /**
     * Constructor of CoverCache.
     *
     * @param context application environment interface.
     */
    public CoverCache(Context context) {
        this.context = context;

        // Get cache directory from parameter.
        cacheDir = new File(context.getCacheDir(), PARAMETER_CACHE_DIRECTORY);

        // Create cache directory.
        createCacheDir();
    }

    /**
     * Check if cache dir exist if not create directory.
     *
     * @return true if cache dir does exist and is created.
     */
    private boolean createCacheDir() {
        return !cacheDir.exists() && cacheDir.mkdirs();
    }

    /**
     * Download the cover with Glide (it can avoid repeating requests) and save the file.
     *
     * @param thumbnailUrl url of thumbnail.
     * @param headers      headers included in Glide request.
     */
    public void save(String thumbnailUrl, LazyHeaders headers) {
        save(thumbnailUrl, headers, null);
    }

    /**
     * Download the cover with Glide (it can avoid repeating requests) and save the file.
     *
     * @param thumbnailUrl url of thumbnail.
     * @param headers      headers included in Glide request.
     * @param imageView    imageView where picture should be displayed.
     */
    private void save(String thumbnailUrl, LazyHeaders headers, ImageView imageView) {

        // Check if url is empty.
        if (TextUtils.isEmpty(thumbnailUrl))
            // Do not try and create the string. Instead... only try to realize the truth. There is no string.
            return;

        // Download the cover with Glide and save the file.
        GlideUrl url = new GlideUrl(thumbnailUrl, headers);
        Glide.with(context)
                .load(url)
                .downloadOnly(new SimpleTarget<File>() {
                    @Override
                    public void onResourceReady(File resource, GlideAnimation<? super File> anim) {
                        try {
                            // Copy the cover from Glide's cache to local cache.
                            copyToLocalCache(thumbnailUrl, resource);

                            // Check if imageView isn't null and show picture in imageView.
                            if (imageView != null) {
                                loadFromCache(imageView, resource);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }


    /**
     * Copy the cover from Glide's cache to local cache.
     *
     * @param thumbnailUrl url of thumbnail.
     * @param source       the cover image.
     * @throws IOException TODO not returned atm?
     */
    private void copyToLocalCache(String thumbnailUrl, File source) throws IOException {
        // Create cache directory and check if directory exist
        createCacheDir();

        // Create destination file.
        File dest = new File(cacheDir, DiskUtils.hashKeyForDisk(thumbnailUrl));


        // Check if file already exists, if true delete it.
        if (dest.exists())
            dest.delete();

        // Write thumbnail image to file.
        InputStream in = new FileInputStream(source);
        try {
            OutputStream out = new FileOutputStream(dest);
            try {
                // Transfer bytes from in to out.
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }


    /**
     * Returns the cover from cache.
     *
     * @param thumbnailUrl the thumbnail url.
     * @return cover image.
     */
    private File getCoverFromCache(String thumbnailUrl) {
        return new File(cacheDir, DiskUtils.hashKeyForDisk(thumbnailUrl));
    }

    /**
     * Delete the cover file from the cache.
     *
     * @param thumbnailUrl the thumbnail url.
     * @return status of deletion.
     */
    public boolean deleteCoverFromCache(String thumbnailUrl) {
        // Check if url is empty.
        if (TextUtils.isEmpty(thumbnailUrl))
            return false;

        // Remove file.
        File file = new File(cacheDir, DiskUtils.hashKeyForDisk(thumbnailUrl));
        return file.exists() && file.delete();
    }

    /**
     * Save or load the image from cache
     *
     * @param imageView    imageView where picture should be displayed.
     * @param thumbnailUrl the thumbnail url.
     * @param headers      headers included in Glide request.
     */
    public void saveOrLoadFromCache(ImageView imageView, String thumbnailUrl, LazyHeaders headers) {
        // If file exist load it otherwise save it.
        File localCover = getCoverFromCache(thumbnailUrl);
        if (localCover.exists()) {
            loadFromCache(imageView, localCover);
        } else {
            save(thumbnailUrl, headers, imageView);
        }
    }

    /**
     * If the image is already in our cache, use it. If not, load it with glide.
     * TODO not used atm.
     *
     * @param imageView    imageView where picture should be displayed.
     * @param thumbnailUrl url of thumbnail.
     * @param headers      headers included in Glide request.
     */
    public void loadFromCacheOrNetwork(ImageView imageView, String thumbnailUrl, LazyHeaders headers) {
        // If localCover exist load it  from cache otherwise load it from network.
        File localCover = getCoverFromCache(thumbnailUrl);
        if (localCover.exists()) {
            loadFromCache(imageView, localCover);
        } else {
            loadFromNetwork(imageView, thumbnailUrl, headers);
        }
    }

    /**
     * Helper method to load the cover from the cache directory into the specified image view.
     *
     * @param imageView imageView where picture should be displayed.
     * @param file      file to load. Must exist!.
     */
    private void loadFromCache(ImageView imageView, File file) {
        Glide.with(context)
                .load(file)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .centerCrop()
                .into(imageView);
    }

    /**
     * Helper method to load the cover from network into the specified image view.
     * It does NOT save the image in cache!
     *
     * @param imageView    imageView where picture should be displayed.
     * @param thumbnailUrl url of thumbnail.
     * @param headers      headers included in Glide request.
     */
    public void loadFromNetwork(ImageView imageView, String thumbnailUrl, LazyHeaders headers) {
        // Check if url is empty.
        if (TextUtils.isEmpty(thumbnailUrl))
            return;

        GlideUrl url = new GlideUrl(thumbnailUrl, headers);
        Glide.with(context)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .centerCrop()
                .into(imageView);
    }

}
