package eu.kanade.tachiyomi.data.track.myanimelist

import okhttp3.Interceptor
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import org.json.JSONObject
import java.lang.Exception

class MyAnimeListInterceptor(private val myanimelist: Myanimelist): Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!myanimelist.isAuthorized) {
            myanimelist.login(myanimelist.getUsername(), myanimelist.getPassword())
                    .onErrorComplete()
                    .subscribe()
        }

        if (!myanimelist.isAuthorized) throw Exception("Failed MAL Authorization")

        var request = chain.request()

        request.body()?.let {
            val contentType = it.contentType().toString()
            val updatedBody = when {
                contentType.contains("x-www-form-urlencoded") -> updateFormBody(it)
                contentType.contains("json") -> updateJsonBody(it)
                else -> it
            }
            request = request.newBuilder().post(updatedBody).build()
        }

        return chain.proceed(request)
    }

    private fun bodyToString(requestBody: RequestBody): String {
        Buffer().use {
            requestBody.writeTo(it)
            return it.readUtf8()
        }
    }

    private fun updateFormBody(requestBody: RequestBody): RequestBody {
        val formString = bodyToString(requestBody)

        return RequestBody.create(requestBody.contentType(), "$formString${if (formString.isNotEmpty()) "&" else ""}${MyanimelistApi.CSRF}=${myanimelist.getCSRF()}")
    }

    private fun updateJsonBody(requestBody: RequestBody): RequestBody {
        val jsonString = bodyToString(requestBody)
        val newBody = JSONObject(jsonString)
                .put(MyanimelistApi.CSRF, myanimelist.getCSRF())

        return RequestBody.create(requestBody.contentType(), newBody.toString())
    }
}