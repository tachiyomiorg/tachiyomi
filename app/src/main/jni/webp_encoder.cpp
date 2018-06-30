#include <jni.h>

extern "C" {
#include <webp/encode.h>
}

#define LOG_TAG "WEBP_ENC"

#ifdef DEBUG
    #include <android/log.h>
    #define LOG(SEV, FMT, ...) __android_log_print(SEV, LOG_TAG, "(%s:%d) " FMT, __func__, __LINE__, ## __VA_ARGS__)
#else
    #define LOG(...)
#endif

#define LOGE(...) LOG(ANDROID_LOG_ERROR, __VA_ARGS__)
#define LOGW(...) LOG(ANDROID_LOG_WARN, __VA_ARGS__)
#define LOGD(...) LOG(ANDROID_LOG_DEBUG, __VA_ARGS__)
#define LOGI(...) LOG(ANDROID_LOG_INFO, __VA_ARGS__)


#define STORAGE_BUF_SIZE 4096

#define LOSSLESS_LEVEL 2
#define LOSSY_QUALITY 90

static jmethodID gOutputStream_write = nullptr;

struct encode_context {
    JNIEnv* env;
    jbyteArray tmp_buf;
    jobject output_stream;
};

extern "C" JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    LOGI("onLoad");
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }

    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_eu_kanade_tachiyomi_data_encoder_WebpEncoder_nativeInitialize(JNIEnv *env, jclass type) {
    static bool initialized = false;

    // TODO: use a mutex since this is not threadsafe
    if (initialized) {
        return true;
    }

    LOGI("nativeInitialize");

    jclass outputStream_class = env->FindClass("java/io/OutputStream");
    if (outputStream_class == nullptr) {
        LOGE("Could not find OutputStream class");
        return false;
    }

    gOutputStream_write = env->GetMethodID(outputStream_class, "write", "([BII)V");
    if (gOutputStream_write == nullptr) {
        LOGE("Could not find write method of OutputStream");
        return false;
    }

    initialized = true;
    return true;
}

static int webp_write_callback(const uint8_t* data, size_t size, const WebPPicture* picture) {
    struct encode_context* ctx = (struct encode_context*) picture->custom_ptr;

    JNIEnv* env = ctx->env;
    jbyteArray tmp_buf = ctx->tmp_buf;
    jobject output_stream = ctx->output_stream;
    jsize buf_cap = env->GetArrayLength(tmp_buf);

    if (buf_cap <= 0) {
        LOGE("Cannot use tmp_buf of size %d", buf_cap);
        return false;
    }

    // Write the data in chunks to the output stream
    while (size > 0) {
        size_t chunk_size = size;
        if (chunk_size > (size_t)buf_cap) {
            chunk_size = (size_t)buf_cap;
        }

        // Update tmp_buf with chunk data
        env->SetByteArrayRegion(tmp_buf, 0, chunk_size, reinterpret_cast<const jbyte*>(data));
        if (env->ExceptionCheck()) {
            env->ExceptionDescribe();
            env->ExceptionClear();
            LOGE("Could not update tmp_buf with chunk data");
            return false;
        }

        // Write the buffer to the output stream
        env->CallVoidMethod(output_stream, gOutputStream_write, tmp_buf, 0, chunk_size);
        if (env->ExceptionCheck()) {
            env->ExceptionDescribe();
            env->ExceptionClear();
            LOGE("Could not write buffer to output stream");
            return false;
        }

        size -= chunk_size;
        data += chunk_size;
    }

    return true;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_eu_kanade_tachiyomi_data_encoder_WebpEncoder_nativeEncode(JNIEnv* env, jclass type,
                                                               jbyteArray rgba, jint width,
                                                               jint height, jint stride,
                                                               jboolean opaque, jboolean lossless,
                                                               jobject output_stream) {

    // Initialize webp config with appropriate preset and quality / level
    WebPConfig webp_config;
    if (!WebPConfigPreset(&webp_config, WEBP_PRESET_DRAWING, LOSSY_QUALITY)) {
        LOGE("WebPConfigPreset failed");
        return false;
    }

    if (lossless && !WebPConfigLosslessPreset(&webp_config, LOSSLESS_LEVEL)) {
        LOGE("WebPConfigLosslessPreset failed");
        return false;
    }

    // Initialize webp picture with writer callback and context struct
    WebPPicture pic;
    WebPPictureInit(&pic);
    pic.width = width;
    pic.height = height;
    pic.writer = webp_write_callback;

    jbyteArray tmp_buf = env->NewByteArray(STORAGE_BUF_SIZE);
    if (tmp_buf == nullptr) {
        LOGE("Could not allocate byteArray of size %d", STORAGE_BUF_SIZE);
        return false;
    }

    struct encode_context ctx = { env, tmp_buf, output_stream };
    pic.custom_ptr = (void*) &ctx;

    // WebP recommends lossy encodes use YUV instead of RGB
    if (lossless) {
        pic.use_argb = 1;
    } else {
        pic.use_argb = 0;
    }

    if (!WebPValidateConfig(&webp_config)) {
        LOGE("WebPValidateConfig failed");
        return false;
    }

    const jbyte* srcBytes = env->GetByteArrayElements(rgba, NULL);
    const uint8_t* src = (uint8_t*) srcBytes;

    // Import the bitmap into a webp picture
    int (*pictureImport)(WebPPicture*, const uint8_t*, int) = nullptr;
    if (opaque) {
        pictureImport = WebPPictureImportRGBX;
    } else {
        pictureImport = WebPPictureImportRGBA;
    }

    if (!pictureImport(&pic, src, stride)) {
        LOGE("pictureImport failed");
        return false;
    }

    // Encode the webp
    if (!WebPEncode(&webp_config, &pic)) {
        LOGE("WebPEncode failed");
        WebPPictureFree(&pic);
        return false;
    }

    WebPPictureFree(&pic);
    return true;
}