package eu.kanade.tachiyomi.network

import com.squareup.duktape.Duktape
import okhttp3.*
import java.io.IOException

class CloudflareInterceptor : Interceptor {

    private val operationPattern = Regex("""setTimeout\(function\(\)\{\s+(var (?:\w,)+f.+?\r?\n[\s\S]+?a\.value =.+?)\r?\n""")
    
    private val passPattern = Regex("""name="pass" value="(.+?)"""")

    private val challengePattern = Regex("""name="jschl_vc" value="(\w+)"""")

    private val sPattern = Regex("""name="s" value="([^"]+)""")

    private val serverCheck = arrayOf("cloudflare-nginx", "cloudflare")

    @Synchronized
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        // Check if Cloudflare anti-bot is on
        if (response.code() == 503 && response.header("Server") in serverCheck) {
            return try {
                chain.proceed(resolveChallenge(response))
            } catch (e: Exception) {
                // Because OkHttp's enqueue only handles IOExceptions, wrap the exception so that
                // we don't crash the entire app
                throw IOException(e)
            }
        }

        return response
    }

    private fun resolveChallenge(response: Response): Request {
        Duktape.create().use { duktape ->
            val originalRequest = response.request()
            val url = originalRequest.url()
            val domain = url.host()
            val content = response.body()!!.string()

            // CloudFlare requires waiting 4 seconds before resolving the challenge
            Thread.sleep(4000)

            val operation = operationPattern.find(content)?.groups?.get(1)?.value
            val challenge = challengePattern.find(content)?.groups?.get(1)?.value
            val pass = passPattern.find(content)?.groups?.get(1)?.value
            val s = sPattern.find(content)?.groups?.get(1)?.value

            if (operation == null || challenge == null || pass == null || s == null) {
                throw Exception("Failed resolving Cloudflare challenge")
            }

            val js = operation
                    .replace(Regex("""a\.value = (.+ \+ t\.length(\).toFixed\(10\))?).+"""), "$1")
                    .replace(Regex("""\s{3,}[a-z](?: = |\.).+"""), "")
                    .replace("t.length", "${domain.length}")
                    .replace("\n", "")

            val result = duktape.evaluate(js) as String

            val cloudflareUrl = HttpUrl.parse("${url.scheme()}://$domain/cdn-cgi/l/chk_jschl")!!
                    .newBuilder()
                    .addQueryParameter("jschl_vc", challenge)
                    .addQueryParameter("pass", pass)
                    .addQueryParameter("s", s)
                    .addQueryParameter("jschl_answer", result)
                    .toString()

            val cloudflareHeaders = originalRequest.headers()
                    .newBuilder()
                    .add("Referer", url.toString())
                    .add("Accept", "text/html,application/xhtml+xml,application/xml")
                    .add("Accept-Language", "en")
                    .build()

            return GET(cloudflareUrl, cloudflareHeaders, cache = CacheControl.Builder().build())
        }
    }

}
