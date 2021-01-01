package eu.kanade.tachiyomi.data.track.kitsu

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Response
import uy.kohesive.injekt.injectLazy

class KitsuInterceptor(val kitsu: Kitsu) : Interceptor {

    private val json: Json by injectLazy()

    /**
     * OAuth object used for authenticated requests.
     */
    private var oauth: OAuth? = kitsu.restoreToken()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val currAuth = oauth ?: throw Exception("Not authenticated with Kitsu")

        val refreshToken = currAuth.refresh_token!!

        // Refresh access token if expired.
        if (currAuth.isExpired()) {
            val response = chain.proceed(KitsuApi.refreshTokenRequest(refreshToken))
            if (response.isSuccessful) {
                newAuth(json.decodeFromString(response.body!!.string()))
            } else {
                response.close()
            }
        }

        // Add the authorization header to the original request.
        val authRequest = originalRequest.newBuilder()
            .addHeader("Authorization", "Bearer ${oauth!!.access_token}")
            .header("Accept", "application/vnd.api+json")
            .header("Content-Type", "application/vnd.api+json")
            .build()

        return chain.proceed(authRequest)
    }

    fun newAuth(oauth: OAuth?) {
        this.oauth = oauth
        kitsu.saveToken(oauth)
    }
}
