package eu.kanade.tachiyomi.data.track.anilist

import okhttp3.Interceptor
import okhttp3.Response


class AnilistInterceptor(val anilist: Anilist, private var token: String?) : Interceptor {

    /**
     * OAuth object used for authenticated requests.
     *
     * Anilist returns the date without milliseconds. We fix that and make the token expire 1 minute
     * before its original expiration date.
     */
    private var oauth: OAuth? = null
        set(value) {
            field = value?.copy(expires = value.expires * 1000 - 60 * 1000)
        }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        if (token.isNullOrEmpty()) {
            throw Exception("Not authenticated with Anilist")
        }
        if (oauth == null){
            oauth = anilist.loadOAuth()
        }
        // Refresh access token if null or expired.
        if (oauth!!.isExpired()) {
            anilist.logout()
            throw Exception("Token expired")
        }

        // Throw on null auth.
        if (oauth == null) {
            throw Exception("No authentication token")
        }

        // Add the authorization header to the original request.
        val authRequest = originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer ${oauth!!.access_token}")
                .build()

        return chain.proceed(authRequest)
    }

    /**
     * Called when the user authenticates with Anilist for the first time. Sets the refresh token
     * and the oauth object.
     */
    fun setAuth(oauth: OAuth?) {
        token = oauth?.access_token
        this.oauth = oauth
        anilist.saveOAuth(oauth)
    }

}