package eu.kanade.tachiyomi.network

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import eu.kanade.tachiyomi.util.WebViewClientCompat
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class CloudflareInterceptor(private val context: Context) : Interceptor {

    private val serverCheck = arrayOf("cloudflare-nginx", "cloudflare")

    private val handler = Handler(Looper.getMainLooper())

    /**
     * When this is called, it initializes the WebView if it wasn't already. We use this to avoid
     * blocking the main thread too much. If used too often we could consider moving it to the
     * Application class.
     */
    private val initWebView by lazy {
        if (Build.VERSION.SDK_INT >= 17) {
            WebSettings.getDefaultUserAgent(context)
        } else {
            null
        }
    }

    @Synchronized
    override fun intercept(chain: Interceptor.Chain): Response {
        initWebView

        val response = chain.proceed(chain.request())

        // Check if Cloudflare anti-bot is on
        if (response.code() == 503 && response.header("Server") in serverCheck) {
            try {
                response.close()
                val solutionRequest = resolveWithWebView(chain.request())
                return chain.proceed(solutionRequest)
            } catch (e: Exception) {
                // Because OkHttp's enqueue only handles IOExceptions, wrap the exception so that
                // we don't crash the entire app
                throw IOException(e)
            }
        }

        return response
    }

    private fun isChallengeSolutionUrl(url: String): Boolean {
        return "chk_jschl" in url
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun resolveWithWebView(request: Request): Request {
        // We need to lock this thread until the WebView finds the challenge solution url, because
        // OkHttp doesn't support asynchronous interceptors.
        val latch = CountDownLatch(1)

        var webView: WebView? = null
        var solutionUrl: String? = null
        var challengeFound = false

        val origRequestUrl = request.url().toString()
        val headers = request.headers().toMultimap().mapValues { it.value.getOrNull(0) ?: "" }

        handler.post {
            val view = WebView(context)
            webView = view
            view.settings.javaScriptEnabled = true
            view.settings.userAgentString = request.header("User-Agent")
            view.webViewClient = object : WebViewClientCompat() {

                override fun shouldOverrideUrlCompat(view: WebView, url: String): Boolean {
                    if (isChallengeSolutionUrl(url)) {
                        solutionUrl = url
                        latch.countDown()
                    }
                    return solutionUrl != null
                }

                override fun shouldInterceptRequestCompat(
                        view: WebView,
                        url: String
                ): WebResourceResponse? {
                    if (solutionUrl != null) {
                        // Intercept any request when we have the solution.
                        return WebResourceResponse("text/plain", "UTF-8", null)
                    }
                    return null
                }

                override fun onPageFinished(view: WebView, url: String) {
                    // Http error codes are only received since M
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                        url == origRequestUrl && !challengeFound
                    ) {
                        // The first request didn't return the challenge, abort.
                        latch.countDown()
                    }
                }

                override fun onReceivedErrorCompat(
                        view: WebView,
                        errorCode: Int,
                        description: String?,
                        failingUrl: String,
                        isMainFrame: Boolean
                ) {
                    if (isMainFrame) {
                        if (errorCode == 503) {
                            // Found the cloudflare challenge page.
                            challengeFound = true
                        } else {
                            // Unlock thread, the challenge wasn't found.
                            latch.countDown()
                        }
                    }
                }
            }
            webView?.loadUrl(origRequestUrl, headers)
        }

        // Wait a reasonable amount of time to retrieve the solution. The minimum should be
        // around 4 seconds but it can take more due to slow networks or server issues.
        latch.await(12, TimeUnit.SECONDS)

        handler.post {
            webView?.stopLoading()
            webView?.destroy()
        }

        val solution = solutionUrl ?: throw Exception("Challenge not found")

        return Request.Builder().get()
            .url(solution)
            .headers(request.headers())
            .addHeader("Referer", origRequestUrl)
            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml")
            .addHeader("Accept-Language", "en")
            .build()
    }

}
