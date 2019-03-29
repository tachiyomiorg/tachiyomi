package eu.kanade.tachiyomi.network

import com.squareup.duktape.Duktape
import okhttp3.*
import java.io.IOException

class CloudflareInterceptor : Interceptor {

    private val operationPattern = Regex("""setTimeout\(function\(\)\{\s+(var (?:\w,)+f.+?\r?\n[\s\S]+?a\.value =.+?)\r?\n""")
    
    private val passPattern = Regex("""name="pass" value="(.+?)"""")

    private val challengePattern = Regex("""name="jschl_vc" value="(\w+)"""")

    private val sPattern = Regex("""name="s" value="([^"]+)""")

    private val kPattern = Regex("""k\s+=\s+'([^']+)';""")

    private val serverCheck = arrayOf("cloudflare-nginx", "cloudflare")

    private interface INativeFunctions {
        fun DecodeBase64(input: String): String
    }

    private val nativeFunctions: INativeFunctions = object : INativeFunctions {
        override fun DecodeBase64(input: String): String {
            return okio.ByteString.decodeBase64(input)!!.utf8()
        }
    }

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

            // If `k` is null, it uses old methods.
            val k = kPattern.find(content)?.groups?.get(1)?.value ?: ""
            val innerHTMLValue = Regex("""<div(?:.*)id="$k"(?:.*)>(.+?)</div>""")
                    .find(content)?.groups?.get(1)?.value ?: ""

            if (operation == null || challenge == null || pass == null || s == null) {
                throw Exception("Failed resolving Cloudflare challenge")
            }

            // Export native functions to js object.
            duktape.set("NativeFunctions", INativeFunctions::class.java, nativeFunctions)

            // Return simulated innerHTML when call DOM.
            val simulatedDocumentJS = """var document = { getElementById: function (x) { return { innerHTML: "$innerHTMLValue" } } }"""

            val js = operation
                    .replace(Regex("""a\.value = (.+\.toFixed\(10\);).+"""), "$1")
                    .replace(Regex("""\s{3,}[a-z](?: = |\.).+"""), "")
                    .replace("t.length", "${domain.length}")
                    .replace("\n", "")

            val result = duktape.evaluate("""$simulatedDocumentJS;$ATOB_JS;$SIMULATED_BROWSER_API;var t="$domain";$js""") as String

            val cloudflareUrl = HttpUrl.parse("${url.scheme()}://$domain/cdn-cgi/l/chk_jschl")!!
                    .newBuilder()
                    .addQueryParameter("s", s)
                    .addQueryParameter("jschl_vc", challenge)
                    .addQueryParameter("pass", pass)
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

    companion object {
        // atob() is browser API, Using native function from okio.
        private const val ATOB_JS = """var atob = function (input) { return NativeFunctions.DecodeBase64(input) }"""
        // Simulated browser API that Cloudflare uses.
        private const val SIMULATED_BROWSER_API = """
                String.prototype.italics = function () { return "<i></i>" };
		        Array.prototype.fill = "function fill() { [native code] }";
            """
    }
}