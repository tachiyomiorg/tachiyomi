package eu.kanade.tachiyomi.data.updater.github

import eu.kanade.tachiyomi.data.updater.UpdateResult

sealed class GithubUpdateResult : UpdateResult() {

    class NewUpdate(release: GithubRelease): UpdateResult.NewUpdate<GithubRelease>(release)
    class NoNewUpdate : UpdateResult.NoNewUpdate()

}
