package eu.kanade.tachiyomi.network

import com.squareup.duktape.Duktape
import okhttp3.CacheControl
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class CloudflareInterceptor : Interceptor {

    private val operationPattern = Regex("""setTimeout\(function\(\)\{\s+(var (?:\w,)+f.+?\r?\n[\s\S]+?a\.value =.+?)\r?\n""")
    
    private val passPattern = Regex("""name="pass" value="(.+?)"""")

    private val challengePattern = Regex("""name="jschl_vc" value="(\w+)"""")

    private val serverCheck = arrayOf("cloudflare-nginx", "cloudflare")

    @Synchronized
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        // Check if Cloudflare anti-bot is on
        if (response.code() == 503 && response.header("Server") in serverCheck) {
            return chain.proceed(resolveChallenge(response))
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

            if (operation == null || challenge == null || pass == null) {
                throw Exception("Failed resolving Cloudflare challenge")
            }

            val js = operation
                    .replace(Regex("""a\.value = (.+ \+ t\.length).+"""), "$1")
                    .replace(Regex("""\s{3,}[a-z](?: = |\.).+"""), "")
                    .replace("t.length", "${domain.length}")
                    .replace("\n", "")

            val result = duktape.evaluate(js) as Double

            val cloudflareUrl = HttpUrl.parse("${url.scheme()}://$domain/cdn-cgi/l/chk_jschl")!!
                    .newBuilder()
                    .addQueryParameter("jschl_vc", challenge)
                    .addQueryParameter("pass", pass)
                    .addQueryParameter("jschl_answer", "$result")
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
