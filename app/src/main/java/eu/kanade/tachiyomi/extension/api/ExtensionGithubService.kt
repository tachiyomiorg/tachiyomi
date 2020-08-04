package eu.kanade.tachiyomi.extension.api

import com.google.gson.JsonArray
import eu.kanade.tachiyomi.network.NetworkHelper
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import uy.kohesive.injekt.injectLazy

/**
 * Used to get the extension repo listing from GitHub.
 */
interface ExtensionGithubService {

    companion object {
        private val client by lazy {
            val network: NetworkHelper by injectLazy()
            network.client.newBuilder()
                .addNetworkInterceptor { chain ->
                    val originalResponse = chain.proceed(chain.request())
                    originalResponse.newBuilder()
                        .header("Content-Encoding", "gzip")
                        .header("Content-Type", "application/json")
                        .build()
                }
                .build()
        }

        fun create(): ExtensionGithubService {
            val adapter = Retrofit.Builder()
                .baseUrl(ExtensionGithubApi.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()

            return adapter.create(ExtensionGithubService::class.java)
        }
    }

    @GET("${ExtensionGithubApi.REPO_URL_PREFIX}index.json.gz")
    suspend fun getRepo(): JsonArray
}
