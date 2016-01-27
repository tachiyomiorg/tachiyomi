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

public class CoverCache {

    private static final String PARAMETER_CACHE_DIRECTORY = "cover_disk_cache";

    private Context context;
    private File cacheDir;

    public CoverCache(Context context) {
        this.context = context;
        cacheDir = new File(context.getCacheDir(), PARAMETER_CACHE_DIRECTORY);
        createCacheDir();
    }

    private boolean createCacheDir() {
        return !cacheDir.exists() && cacheDir.mkdirs();
    }

    public void save(String thumbnailUrl, LazyHeaders headers) {
        save(thumbnailUrl, headers, null);
    }

    // Download the cover with Glide (it can avoid repeating requests) and save the file on this cache
    // Optionally, load the image in the given image view when the resource is ready, if not null
    public void save(String thumbnailUrl, LazyHeaders headers, ImageView imageView) {
        if (TextUtils.isEmpty(thumbnailUrl))
            return;

        GlideUrl url = new GlideUrl(thumbnailUrl, headers);
        Glide.with(context)
                .load(url)
                .downloadOnly(new SimpleTarget<File>() {
                    @Override
                    public void onResourceReady(File resource, GlideAnimation<? super File> anim) {
                        try {
                            add(thumbnailUrl, resource);
                            if (imageView != null) {
                                loadFromCache(imageView, resource);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    // Copy the cover from Glide's cache to this cache
    public void add(String thumbnailUrl, File source) throws IOException {
        createCacheDir();
        File dest = new File(cacheDir, DiskUtils.hashKeyForDisk(thumbnailUrl));
        if (dest.exists())
            dest.delete();

        InputStream in = new FileInputStream(source);
        try {
            OutputStream out = new FileOutputStream(dest);
            try {
                // Transfer bytes from in to out
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

    // Get the cover from cache
    public File get(String thumbnailUrl) {
        return new File(cacheDir, DiskUtils.hashKeyForDisk(thumbnailUrl));
    }

    // Delete the cover from cache
    public boolean delete(String thumbnailUrl) {
        if (TextUtils.isEmpty(thumbnailUrl))
            return false;

        File file = new File(cacheDir, DiskUtils.hashKeyForDisk(thumbnailUrl));
        return file.exists() && file.delete();
    }

    // Save and load the image from cache
    public void saveAndLoadFromCache(ImageView imageView, String thumbnailUrl, LazyHeaders headers) {
        File localCover = get(thumbnailUrl);
        if (localCover.exists()) {
            loadFromCache(imageView, localCover);
        } else {
            save(thumbnailUrl, headers, imageView);
        }
    }

    // If the image is already in our cache, use it. If not, load it with glide
    public void loadFromCacheOrNetwork(ImageView imageView, String thumbnailUrl, LazyHeaders headers) {
        File localCover = get(thumbnailUrl);
        if (localCover.exists()) {
            loadFromCache(imageView, localCover);
        } else {
            loadFromNetwork(imageView, thumbnailUrl, headers);
        }
    }

    // Helper method to load the cover from the cache directory into the specified image view
    // The file must exist
    private void loadFromCache(ImageView imageView, File file) {
        Glide.with(context)
                .load(file)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .centerCrop()
                .into(imageView);
    }

    // Helper method to load the cover from network into the specified image view.
    // It does NOT save the image in cache
    public void loadFromNetwork(ImageView imageView, String thumbnailUrl, LazyHeaders headers) {
        GlideUrl url = new GlideUrl(thumbnailUrl, headers);
        Glide.with(context)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .centerCrop()
                .into(imageView);
    }

}
