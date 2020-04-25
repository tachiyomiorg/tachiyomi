package eu.kanade.tachiyomi.network

import android.content.Context
import java.io.File
import okhttp3.Cache
import okhttp3.OkHttpClient

class NetworkHelper(context: Context) {

    private val cacheDir = File(context.cacheDir, "network_cache")

    private val cacheSize = 5L * 1024 * 1024 // 5 MiB

    val cookieManager = AndroidCookieJar()

    val client = OkHttpClient.Builder()
        .cookieJar(cookieManager)
        .cache(Cache(cacheDir, cacheSize))
        .build()

    val cloudflareClient = client.newBuilder()
        .addInterceptor(UserAgentInterceptor())
        .addInterceptor(CloudflareInterceptor(context))
        .build()
}
