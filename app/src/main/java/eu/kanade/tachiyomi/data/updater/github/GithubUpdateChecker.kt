package eu.kanade.tachiyomi.data.updater.github

import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.updater.UpdateResult
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.injectLazy

class GithubUpdateChecker {

    private val networkService: NetworkHelper by injectLazy()

    private val repo: String by lazy {
        if (BuildConfig.DEBUG) {
            "tachiyomiorg/tachiyomi-preview"
        } else {
            "tachiyomiorg/tachiyomi"
        }
    }

    suspend fun checkForUpdate(): UpdateResult {
        return withContext(Dispatchers.IO) {
            networkService.client
                .newCall(GET("https://api.github.com/repos/$repo/releases/latest"))
                .await()
                .parseAs<GithubRelease>()
                .let {
                    // Check if latest version is different from current version
                    if (isNewVersion(it.version)) {
                        GithubUpdateResult.NewUpdate(it)
                    } else {
                        GithubUpdateResult.NoNewUpdate()
                    }
                }
        }
    }

    private fun isNewVersion(versionTag: String): Boolean {
        // Removes prefixes like "r" or "v"
        val newVersion = versionTag.replace("[^\\d.]".toRegex(), "")

        return if (BuildConfig.DEBUG) {
            // Preview builds: based on releases in "tachiyomiorg/tachiyomi-preview" repo
            // tagged as something like "r1234"
            newVersion.toInt() > BuildConfig.COMMIT_COUNT.toInt()
        } else {
            // Release builds: based on releases in "tachiyomiorg/tachiyomi" repo
            // tagged as something like "v0.1.2"
            newVersion != BuildConfig.VERSION_NAME
        }
    }
}
