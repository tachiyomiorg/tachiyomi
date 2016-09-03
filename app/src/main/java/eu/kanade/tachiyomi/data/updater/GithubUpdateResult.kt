package eu.kanade.tachiyomi.data.updater

sealed class GithubUpdateResult {

    class NewUpdate(val release: GithubRelease): GithubUpdateResult()
    class NoNewUpdate(): GithubUpdateResult()
}