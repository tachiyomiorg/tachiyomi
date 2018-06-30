package eu.kanade.tachiyomi.data.encoder;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import android.util.Log;

import timber.log.Timber;

public class WebpEncoder {
    private static boolean initialized = false;

    static {
        System.loadLibrary("webp_encoder");
        initialized = nativeInitialize();
    }

    public static boolean isInitialized() {
        return initialized;
    }

    private static boolean validateBitmap(@NonNull Bitmap bitmap) {
        // Currently only ARGB_8888 bitmaps are supported, they are the most common format
        return Bitmap.Config.ARGB_8888.equals(bitmap.getConfig());
    }

    public static boolean encode(@NonNull Bitmap bitmap, boolean lossless, @NonNull OutputStream out) {
        if (!isInitialized()) {
            Timber.e("WebpEncoder not initialized");
            return false;
        }

        if (!validateBitmap(bitmap)) {
            Timber.e("Bitmap config %s is not supported", bitmap.getConfig());
            return false;
        }

        int bpp = 4; // ARGB_8888 is 4 bytes per pixel

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int stride = bpp * width;
        int size = stride * height;
        boolean opaque = !bitmap.hasAlpha();

        if (stride != bitmap.getRowBytes()) {
            Timber.e("Stride of %d does not match rowBytes %d", stride, bitmap.getRowBytes());
            return false;
        }

        ByteBuffer byteBuffer = ByteBuffer.allocate(size);
        bitmap.copyPixelsToBuffer(byteBuffer);

        return nativeEncode(byteBuffer.array(), width, height, stride, opaque, lossless, out);
    }

    private native static boolean nativeInitialize();

    private native static boolean nativeEncode(@NonNull byte[] rgba, int width, int height,
                                               int stride, boolean opaque, boolean lossless,
                                               @NonNull OutputStream out);
}