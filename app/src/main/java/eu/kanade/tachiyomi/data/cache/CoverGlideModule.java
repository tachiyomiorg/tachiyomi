package eu.kanade.tachiyomi.data.cache;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.module.GlideModule;

/**
 * Class used to update Glide module settings
 */
public class CoverGlideModule implements GlideModule {


    /**
     * Bitmaps decoded from most image formats (other than GIFs with hidden configs), will be decoded with the
     * ARGB_8888 config.
     */
    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
        builder.setDecodeFormat(DecodeFormat.PREFER_ARGB_8888);
    }

    @Override
    public void registerComponents(Context context, Glide glide) {
        // Nothing to see here!
    }
}
