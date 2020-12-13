package eu.kanade.tachiyomi.data.updater.github

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Used to connect with the GitHub API to get the latest release version from a repo.
 */
interface GithubService {

    companion object {
        fun create(): GithubService {
            val restAdapter = Retrofit.Builder()
                .baseUrl("https://api.github.com")
                .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
                .client(Injekt.get<NetworkHelper>().client)
                .build()

            return restAdapter.create(GithubService::class.java)
        }
    }

    @GET("/repos/{repo}/releases/latest")
    suspend fun getLatestVersion(@Path("repo", encoded = true) repo: String): GithubRelease
}
