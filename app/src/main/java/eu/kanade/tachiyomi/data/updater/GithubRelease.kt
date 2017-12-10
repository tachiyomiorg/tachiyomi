package eu.kanade.tachiyomi.data.updater

import com.google.gson.annotations.SerializedName

/**
 * Release object.
 * Contains information about the latest release from Github.
 *
 * @param version version of latest release.
 * @param changeLog log of latest release.
 * @param assets assets of latest release.
 */
class GithubRelease(@SerializedName("tag_name") val version: String,
                    @SerializedName("body") val changeLog: String,
                    @SerializedName("assets") private val assets: List<Assets>) {

    /**
     * Get download link of latest release from the assets.
     * @return download link of latest release.
     */
    val downloadLink: String
        get() = assets[0].downloadLink

    /**
     * Assets class containing download url.
     * @param downloadLink download url.
     */
    inner class Assets(@SerializedName("browser_download_url") val downloadLink: String)
}

