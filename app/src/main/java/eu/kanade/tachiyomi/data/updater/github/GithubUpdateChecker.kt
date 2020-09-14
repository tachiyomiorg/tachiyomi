package eu.kanade.tachiyomi.data.updater.github

import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.updater.UpdateResult

class GithubUpdateChecker {

    private val service: GithubService = GithubService.create()

    private val repo: String by lazy {
        if (BuildConfig.DEBUG) {
            "tachiyomiorg/android-app-preview"
        } else {
            "inorichi/tachiyomi"
        }
    }

    suspend fun checkForUpdate(): UpdateResult {
        val release = service.getLatestVersion(repo)

        // Check if latest version is different from current version
        return if (isNewVersion(release.version)) {
            GithubUpdateResult.NewUpdate(release)
        } else {
            GithubUpdateResult.NoNewUpdate()
        }
    }

    private fun isNewVersion(versionTag: String): Boolean {
        // Removes prefixes like "r" or "v"
        val newVersion = versionTag.replace("[^\\d.]".toRegex(), "")

        return if (BuildConfig.DEBUG) {
            // Preview builds: based on releases in "tachiyomiorg/android-app-preview" repo
            // tagged as something like "r1234"
            newVersion.toInt() > BuildConfig.COMMIT_COUNT.toInt()
        } else {
            // Release builds: based on releases in "inorichi/tachiyomi" repo
            // tagged as something like "v0.1.2"
            newVersion != BuildConfig.VERSION_NAME
        }
    }
}
