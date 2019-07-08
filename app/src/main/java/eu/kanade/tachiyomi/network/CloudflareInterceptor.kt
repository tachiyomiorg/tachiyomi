package eu.kanade.tachiyomi.network

import android.util.Log
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.URL
import java.util.regex.Pattern

class CloudFlareInterceptor: Interceptor {
    private val serverCheck = arrayOf("cloudflare-nginx", "cloudflare")

    @Synchronized
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        if (response.code() == 503 && response.header("Server") in serverCheck) {
            try {
                val solutionRequest = resolve(response, chain.request())
                response.close()
                return chain.proceed(solutionRequest)
            } catch (e: Exception) {
                throw IOException(e)
            }
        }

        return response
    }

    private fun isChallengeSolutionUrl(url: String): Boolean {
        return "chk_jschl" in url
    }

    private fun resolve(response: Response, request: Request): Request {
        val origRequestUrl = request.url().toString()
        val body = response.body()?.string() ?: ""
        val headers = Headers.Builder()
        if(response.headers().get("Set-Cookie") != null) {
            headers.add("Cookie", response.headers().get("Set-Cookie") ?: "")
        }
        headers.addAll(request.headers())

        Log.i("AnimeHub", "${headers.build()}")

        if(!isChallengeSolutionUrl(body)) throw Exception("Challenge not found")

        val re1 = Pattern.compile("name=['|\"]s['|\"][\\s]+value=['|\"](.*?)['|\"]").matcher(body)
        val s: String
        if(re1.find()) {
            s = re1.group(1)
        } else {
            throw Exception("s not found")
        }

        val re2 = Pattern.compile("name=['|\"]jschl_vc['|\"][\\s]+value=['|\"](.*?)['|\"]").matcher(body)
        val jschlVc: String
        if(re2.find()) {
            jschlVc = re2.group(1)
        } else {
            throw Exception("jschl_vc not found")
        }

        val re3 = Pattern.compile("name=['|\"]pass['|\"][\\s]+value=['|\"](.*?)['|\"]").matcher(body)
        val pass: String
        if(re3.find()) {
            pass = re3.group(1)
        } else {
            throw Exception("pass not found")
        }

        val jschlAnswer = CloudFlareAnswer.cfa(body) ?: throw Exception("jschl_answer not found")

        val url = URL(origRequestUrl)
        val base = "${url.protocol}://${url.host}"
        val solve = "$base/cdn-cgi/l/chk_jschl?s=$s&jschl_vc=$jschlVc&pass=$pass&jschl_answer=$jschlAnswer"

        return Request.Builder().get()
            .url(solve)
            .headers(headers.build())
            .addHeader("Referer", origRequestUrl)
            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml")
            .addHeader("Accept-Language", "en")
            .build()
    }
}
