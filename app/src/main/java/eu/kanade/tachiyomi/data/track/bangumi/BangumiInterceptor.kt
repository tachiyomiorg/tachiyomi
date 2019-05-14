package eu.kanade.tachiyomi.data.track.bangumi

import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.Response

class BangumiInterceptor(val bangumi: Bangumi, val gson: Gson) : Interceptor {

    /**
     * OAuth object used for authenticated requests.
     */
    private var oauth: OAuth? = bangumi.restoreToken()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val currAuth = oauth ?: throw Exception("Not authenticated with Bangumi")

        val refreshToken = currAuth.refresh_token!!

        // Refresh access token if expired.
        if (currAuth.isExpired()) {
            val response = chain.proceed(BangumiApi.refreshTokenRequest(refreshToken))
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
        bangumi.saveToken(oauth)
    }
}
