package eu.kanade.tachiyomi.network

import android.content.Context
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File

class NetworkHelper(context: Context) {

    private val cacheDir = File(context.cacheDir, "network_cache")

    private val cacheSize = 5L * 1024 * 1024 // 5 MiB

    private val cookieManager = PersistentCookieJar(context)

    val client = OkHttpClient.Builder().addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
            .cookieJar(cookieManager)
            .cache(Cache(cacheDir, cacheSize))
            .build()

    val cloudflareClient = client.newBuilder()
            .addInterceptor(CloudflareInterceptor())
            .build()

    val cookies: PersistentCookieStore
        get() = cookieManager.store

}
