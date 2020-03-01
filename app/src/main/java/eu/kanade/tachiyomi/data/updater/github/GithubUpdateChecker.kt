package eu.kanade.tachiyomi.data.updater.github

import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.updater.UpdateChecker
import eu.kanade.tachiyomi.data.updater.UpdateResult

class GithubUpdateChecker : UpdateChecker() {

    private val service: GithubService = GithubService.create()

    override suspend fun checkForUpdate(): UpdateResult {
        val release = service.getLatestVersion()

        val newVersion = release.version.replace("[^\\d.]".toRegex(), "")

        // Check if latest version is different from current version
        return if (newVersion != BuildConfig.VERSION_NAME) {
            GithubUpdateResult.NewUpdate(release)
        } else {
            GithubUpdateResult.NoNewUpdate()
        }
    }
}
