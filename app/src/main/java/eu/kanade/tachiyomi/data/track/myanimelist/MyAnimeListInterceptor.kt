package eu.kanade.tachiyomi.data.track.myanimelist

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Response
import uy.kohesive.injekt.injectLazy

class MyAnimeListInterceptor(private val myanimelist: MyAnimeList, private var token: String?) : Interceptor {

    private val json: Json by injectLazy()

    private var oauth: OAuth? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        if (token.isNullOrEmpty()) {
            throw Exception("Not authenticated with MyAnimeList")
        }
        if (oauth == null) {
            oauth = myanimelist.loadOAuth()
        }
        // Refresh access token if expired
        if (oauth != null && oauth!!.isExpired()) {
            chain.proceed(MyAnimeListApi.refreshTokenRequest(oauth!!.refresh_token)).use {
                if (it.isSuccessful) {
                    setAuth(json.decodeFromString(it.body!!.string()))
                }
            }
        }
        if (oauth == null) {
            throw Exception("No authentication token")
        }

        // Add the authorization header to the original request
        val authRequest = originalRequest.newBuilder()
            .addHeader("Authorization", "Bearer ${oauth!!.access_token}")
            .build()

        return chain.proceed(authRequest)
    }

    /**
     * Called when the user authenticates with MyAnimeList for the first time. Sets the refresh token
     * and the oauth object.
     */
    fun setAuth(oauth: OAuth?) {
        token = oauth?.access_token
        this.oauth = oauth
        myanimelist.saveOAuth(oauth)
    }
}
