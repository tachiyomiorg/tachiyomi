package eu.kanade.tachiyomi.data.updater.github

import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.updater.UpdateChecker
import eu.kanade.tachiyomi.data.updater.UpdateResult
import rx.Observable

class GithubUpdateChecker : UpdateChecker() {

    private val service: GithubService = GithubService.create()

    override fun checkForUpdate(): Observable<UpdateResult> {
        return service.getLatestVersion().map { release ->
            val newVersion = release.version.replace("[^\\d.]".toRegex(), "")

            // Check if latest version is different from current version
            if (newVersion != BuildConfig.VERSION_NAME) {
                GithubUpdateResult.NewUpdate(release)
            } else {
                GithubUpdateResult.NoNewUpdate()
            }
        }
    }

}
