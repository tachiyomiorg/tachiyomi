package eu.kanade.tachiyomi.data.updater.github

import eu.kanade.tachiyomi.network.NetworkHelper
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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
                .addConverterFactory(GsonConverterFactory.create())
                .client(Injekt.get<NetworkHelper>().client)
                .build()

            return restAdapter.create(GithubService::class.java)
        }
    }

    @GET("/repos/{repo}/releases/latest")
    suspend fun getLatestVersion(@Path("repo", encoded = true) repo: String): GithubRelease
}
