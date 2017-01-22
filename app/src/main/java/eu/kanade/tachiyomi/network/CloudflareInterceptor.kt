package eu.kanade.tachiyomi.network

import com.squareup.duktape.Duktape
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class CloudflareInterceptor(private val cookies: PersistentCookieStore) : Interceptor {

    //language=RegExp
    private val operationPattern = Regex("""setTimeout\(function\(\)\{\s+(var (?:\w,)+f.+?\r?\n[\s\S]+?a\.value =.+?)\r?\n""")
    
    //language=RegExp
    private val passPattern = Regex("""name="pass" value="(.+?)"""")

    //language=RegExp
    private val challengePattern = Regex("""name="jschl_vc" value="(\w+)"""")

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        // Check if we already solved a challenge
        if (response.code() != 503 &&
                cookies.get(response.request().url()).any { it.name() == "cf_clearance" }) {
            return response
        }

        // Check if Cloudflare anti-bot is on
        if ("URL=/cdn-cgi/" in response.header("Refresh", "")
                && response.header("Server", "") == "cloudflare-nginx") {
            return chain.proceed(resolveChallenge(response))
        }

        return response
    }

    private fun resolveChallenge(response: Response): Request {
        val duktape = Duktape.create()
        try {
            val originalRequest = response.request()
            val domain = originalRequest.url().host()
            val content = response.body().string()

            // CloudFlare requires waiting 4 seconds before resolving the challenge
            Thread.sleep(4000)

            val operation = operationPattern.find(content)?.groups?.get(1)?.value
            val challenge = challengePattern.find(content)?.groups?.get(1)?.value
            val pass = passPattern.find(content)?.groups?.get(1)?.value

            if (operation == null || challenge == null || pass == null) {
                throw RuntimeException("Failed resolving Cloudflare challenge")
            }

            val js = operation
                    //language=RegExp
                    .replace(Regex("""a\.value =(.+?) \+.*"""), "$1")
                    //language=RegExp
                    .replace(Regex("""\s{3,}[a-z](?: = |\.).+"""), "")
                    .replace("\n", "")

            val result = (duktape.evaluate(js) as Double).toInt()

            val answer = "${result + domain.length}"

            val url = HttpUrl.parse("http://$domain/cdn-cgi/l/chk_jschl").newBuilder()
                    .addQueryParameter("jschl_vc", challenge)
                    .addQueryParameter("pass", pass)
                    .addQueryParameter("jschl_answer", answer)
                    .toString()

            val referer = originalRequest.url().toString()
            return GET(url, originalRequest.headers().newBuilder().add("Referer", referer).build())
        } finally {
            duktape.close()
        }
    }

}