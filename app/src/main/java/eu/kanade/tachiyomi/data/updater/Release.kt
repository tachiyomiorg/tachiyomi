package eu.kanade.tachiyomi.data.updater

interface Release {

    val info: String

    /**
     * Get download link of latest release.
     * @return download link of latest release.
     */
    val downloadLink: String

}
