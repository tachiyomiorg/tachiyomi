package eu.kanade.tachiyomi.data.network

import android.content.Context
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import rx.Observable
import rx.subscriptions.Subscriptions
import java.io.File
import java.io.IOException

class NetworkHelper(context: Context) {

    private val cacheDir = File(context.cacheDir, "network_cache")

    private val cacheSize = 5L * 1024 * 1024 // 5 MiB

    private val cookieManager = PersistentCookieJar(context)

    val defaultClient = OkHttpClient.Builder()
            .cookieJar(cookieManager)
            .cache(Cache(cacheDir, cacheSize))
            .build()

    val forceCacheClient = defaultClient.newBuilder()
            .addNetworkInterceptor({ chain ->
                val originalResponse = chain.proceed(chain.request())
                originalResponse.newBuilder()
                        .removeHeader("Pragma")
                        .header("Cache-Control", "max-age=" + 600)
                        .build()
            })
            .build()

    val cloudflareClient = defaultClient.newBuilder()
            .addInterceptor { CloudflareScraper.request(it, cookies) }
            .build()

    val cookies: PersistentCookieStore
        get() = cookieManager.store

    fun request(request: Request, client: OkHttpClient = defaultClient): Observable<Response> {
        return Observable.create { subscriber ->
            if (!subscriber.isUnsubscribed) {
                val call = client.newCall(request)
                subscriber.add(Subscriptions.create { call.cancel() })

                try {
                    val response = call.execute()
                    subscriber.add(Subscriptions.create { response.body().close() })
                    subscriber.onNext(response)
                    subscriber.onCompleted()
                } catch (error: IOException) {
                    subscriber.onError(error)
                }
            }
        }
    }

    fun requestBody(request: Request, client: OkHttpClient = defaultClient): Observable<String> {
        return Observable.fromCallable {
            client.newCall(request).execute().body().string()
        }
    }

    fun requestBodyProgress(request: Request, listener: ProgressListener): Observable<Response> {
        return Observable.fromCallable { requestBodyProgressBlocking(request, listener) }
    }

    fun requestBodyProgressBlocking(request: Request, listener: ProgressListener): Response {
        val progressClient = defaultClient.newBuilder()
                .cache(null)
                .addNetworkInterceptor { chain ->
                    val originalResponse = chain.proceed(chain.request())
                    originalResponse.newBuilder()
                            .body(ProgressResponseBody(originalResponse.body(), listener))
                            .build()
                }
                .build()

        return progressClient.newCall(request).execute()
    }

}
