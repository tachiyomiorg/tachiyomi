package eu.kanade.tachiyomi.data.updater.devrepo

import eu.kanade.tachiyomi.data.updater.Release

class DevRepoRelease(override val info: String) : Release {

    override val downloadLink: String
        get() = LATEST_URL

    companion object {
        const val LATEST_URL = "https://tachiyomi.kanade.eu/latest"
    }

}
