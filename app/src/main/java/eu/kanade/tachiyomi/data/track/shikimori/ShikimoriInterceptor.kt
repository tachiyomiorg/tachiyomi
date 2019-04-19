package eu.kanade.tachiyomi.data.track.shikimori

import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.Response

class ShikimoriInterceptor(val shikimori: Shikimori, val gson: Gson) : Interceptor {

    /**
     * OAuth object used for authenticated requests.
     */
    private var oauth: OAuth? = shikimori.restoreToken()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val currAuth = oauth ?: throw Exception("Not authenticated with Shikimori")

        val refreshToken = currAuth.refresh_token!!

        // Refresh access token if expired.
        if (currAuth.isExpired()) {
            val response = chain.proceed(ShikimoriApi.refreshTokenRequest(refreshToken))
            if (response.isSuccessful) {
                newAuth(gson.fromJson(response.body()!!.string(), OAuth::class.java))
            } else {
                response.close()
            }
        }
        // Add the authorization header to the original request.
        val authRequest = originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer ${oauth!!.access_token}")
                .header("User-Agent", "Tachiyomi")
                .build()

        return chain.proceed(authRequest)
    }

    fun newAuth(oauth: OAuth?) {
        this.oauth = oauth
        shikimori.saveToken(oauth)
    }
}
