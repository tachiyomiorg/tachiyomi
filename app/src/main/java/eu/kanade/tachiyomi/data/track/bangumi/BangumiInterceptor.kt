package eu.kanade.tachiyomi.data.track.bangumi

import com.google.gson.Gson
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.Response

class BangumiInterceptor(val bangumi: Bangumi, val gson: Gson) : Interceptor {

  /**
   * OAuth object used for authenticated requests.
   */
  private var oauth: OAuth? = bangumi.restoreToken()

  fun addTocken(tocken: String, oidFormBody: FormBody): FormBody {
    val newFormBody = FormBody.Builder()
    for (i in 0 until oidFormBody.size()) {
      newFormBody.add(oidFormBody.name(i), oidFormBody.value(i))
    }
    newFormBody.add("access_token", tocken)
    return newFormBody.build()
  }

  override fun intercept(chain: Interceptor.Chain): Response {
    val originalRequest = chain.request()

    val currAuth = oauth ?: throw Exception("Not authenticated with Bangumi")

    if (currAuth.isExpired()) {
      val response = chain.proceed(BangumiApi.refreshTokenRequest(currAuth.refresh_token!!))
      if (response.isSuccessful) {
        newAuth(gson.fromJson(response.body()!!.string(), OAuth::class.java))
      } else {
        response.close()
      }
    }

    var authRequest = if (originalRequest.method() == "GET") originalRequest.newBuilder()
      .header("User-Agent", "Tachiyomi")
      .url(originalRequest.url().newBuilder()
        .addQueryParameter("access_token", currAuth.access_token).build())
      .build() else originalRequest.newBuilder()
      .post(addTocken(currAuth.access_token, originalRequest.body() as FormBody))
      .header("User-Agent", "Tachiyomi")
      .build()

    return chain.proceed(authRequest)
  }

  fun newAuth(oauth: OAuth?) {
    this.oauth = if (oauth == null) null else OAuth(
      oauth.access_token,
      oauth.token_type,
      System.currentTimeMillis() / 1000,
      oauth.expires_in,
      oauth.refresh_token,
      this.oauth?.user_id)

    bangumi.saveToken(oauth)
  }
}
